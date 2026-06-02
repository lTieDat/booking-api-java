package com.example.bookingapi.features.booking.service.impl;

import com.example.bookingapi.common.exception.BadRequestException;
import com.example.bookingapi.common.exception.ConflictException;
import com.example.bookingapi.common.exception.ResourceNotFoundException;
import com.example.bookingapi.common.exception.UnauthorizedException;
import com.example.bookingapi.features.auth.model.enums.ActorType;
import com.example.bookingapi.features.booking.dto.request.BookingGuestRequest;
import com.example.bookingapi.features.booking.model.BookedRoom;
import com.example.bookingapi.features.booking.model.Booking;
import com.example.bookingapi.features.booking.model.BookingStatusLog;
import com.example.bookingapi.features.booking.model.CancellationPolicy;
import com.example.bookingapi.features.booking.model.Discount;
import com.example.bookingapi.features.booking.model.Guest;
import com.example.bookingapi.features.booking.model.enums.InventoryHoldStatus;
import com.example.bookingapi.features.booking.model.enums.CancellationPenaltyType;
import com.example.bookingapi.features.booking.model.enums.DiscountType;
import com.example.bookingapi.features.booking.repository.GuestRepository;
import com.example.bookingapi.features.booking.repository.CancellationPolicyRepository;
import com.example.bookingapi.features.booking.repository.DiscountRepository;
import com.example.bookingapi.features.room.model.RoomType;
import com.example.bookingapi.features.user.model.User;
import com.example.bookingapi.features.booking.model.enums.BookingStatus;
import com.example.bookingapi.features.booking.dto.request.BookedRoomRequest;
import com.example.bookingapi.features.booking.dto.request.BookingRequest;
import com.example.bookingapi.features.booking.dto.request.BookingStatusUpdateRequest;
import com.example.bookingapi.features.booking.dto.request.CancelBookingRequest;
import com.example.bookingapi.common.response.ApiMessageResponse;
import com.example.bookingapi.features.booking.dto.response.BookedRoomResponse;
import com.example.bookingapi.features.booking.dto.response.BookingResponse;
import com.example.bookingapi.common.response.PagedResponse;
import com.example.bookingapi.features.booking.repository.BookingRepository;
import com.example.bookingapi.features.booking.repository.BookingStatusLogRepository;
import com.example.bookingapi.features.room.repository.RoomTypeRepository;
import com.example.bookingapi.features.user.repository.UserRepository;
import com.example.bookingapi.common.security.UserPrincipal;
import com.example.bookingapi.features.booking.service.BookingCreationResult;
import com.example.bookingapi.features.booking.service.BookingIdempotencyCacheService;
import com.example.bookingapi.features.booking.service.BookingIdempotencyState;
import com.example.bookingapi.features.booking.service.BookingRequestHashService;
import com.example.bookingapi.features.booking.service.InventoryService;
import com.example.bookingapi.features.booking.service.BookingStateMachine;
import com.example.bookingapi.features.booking.service.BookingService;
import com.example.bookingapi.features.receptionist.service.ReceptionistAccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingServiceImpl.class);
    private static final int IDEMPOTENCY_KEY_MAX_LENGTH = 120;

    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingStatusLogRepository bookingStatusLogRepository;
    @Autowired private RoomTypeRepository roomTypeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private DiscountRepository discountRepository;
    @Autowired private CancellationPolicyRepository cancellationPolicyRepository;
    @Autowired private BookingStateMachine bookingStateMachine;
    @Autowired private GuestRepository guestRepository ;
    @Autowired private InventoryService inventoryService;
    @Autowired private BookingRequestHashService bookingRequestHashService;
    @Autowired private BookingIdempotencyCacheService bookingIdempotencyCacheService;
    @Autowired private ReceptionistAccessService receptionistAccessService;
    @Autowired private TransactionTemplate transactionTemplate;

    @Value("${app.booking.pending-expiration-minutes:15}")
    private long pendingExpirationMinutes;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public BookingCreationResult createBooking(BookingRequest request, UserPrincipal currentUser, String clientRequestId) {
        //normalized idempotency input
        String normalizedClientRequestId = normalizeClientRequestId(clientRequestId);
        String requestHash = bookingRequestHashService.hash(request);

        //step 1: check redis:
        // Case 1: Complete + same hash -> return old booking
        // Case 2: Processing + same hash -> throw conflict exception (means old request is still processing)
        // other case that hash is diff -> conflict exception (means old request is completed but with different content, possible client error of reusing idempotency key for different request)
        Optional<BookingResponse> cachedResponse = resolveCachedIdempotencyState(
                currentUser.getId(), normalizedClientRequestId, requestHash);
        if (cachedResponse.isPresent()) {
            return BookingCreationResult.reused(cachedResponse.get());
        }

        // step 2: check db for existing booking with same clientRequestId (in case redis cache missed or failed)
        Optional<BookingResponse> existingResponse = findExistingBookingResponse(
                currentUser.getId(), normalizedClientRequestId, requestHash);
        if (existingResponse.isPresent()) {
            safePutCompleted(currentUser.getId(), normalizedClientRequestId, requestHash, existingResponse.get().getId());
            return BookingCreationResult.reused(existingResponse.get());
        }

        // step 3: locking processing redis
        //  Case 1: success -> continue creating new booking
        //  Case 2: fail -> means another request with same clientRequestId is processing, throw conflict exception
        boolean processingLocked = safePutProcessingIfAbsent(currentUser.getId(), normalizedClientRequestId, requestHash);
        if (!processingLocked) {
            return resolveCachedIdempotencyState(currentUser.getId(), normalizedClientRequestId, requestHash)
                    .map(BookingCreationResult::reused)
                    .orElseThrow(() -> new ConflictException("Booking request is already processing"));
        }

        try {
            // step 4: create booking in transaction, if success put completed state in redis, if fail remove redis state to allow retry
            BookingResponse response = transactionTemplate.execute(status -> createBookingTransactional(
                    request, currentUser, normalizedClientRequestId, requestHash));
            safePutCompleted(currentUser.getId(), normalizedClientRequestId, requestHash, response.getId());
            return BookingCreationResult.created(response);
        } catch (DataIntegrityViolationException ex) {
            Optional<BookingResponse> fallbackResponse = findExistingBookingResponse(
                    currentUser.getId(), normalizedClientRequestId, requestHash);
            if (fallbackResponse.isPresent()) {
                safePutCompleted(currentUser.getId(), normalizedClientRequestId, requestHash, fallbackResponse.get().getId());
                return BookingCreationResult.reused(fallbackResponse.get());
            }
            safeDeleteIdempotencyState(currentUser.getId(), normalizedClientRequestId);
            throw ex;
        } catch (RuntimeException ex) {
            safeDeleteIdempotencyState(currentUser.getId(), normalizedClientRequestId);
            throw ex;
        }
    }

    private BookingResponse createBookingTransactional(
            BookingRequest request,
            UserPrincipal currentUser,
            String clientRequestId,
            String requestHash
    ) {
        // check if checkin date > checkout date
        if(!request.getCheckInDate().isBefore(request.getCheckOutDate())) {
            throw new BadRequestException("Check-in date must be before check-out date");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        long nights = ChronoUnit.DAYS.between(
                request.getCheckInDate().toLocalDate(),
                request.getCheckOutDate().toLocalDate()
        );
        if (nights <= 0) {
            throw new BadRequestException("Booking must be at least one night");
        }

        Guest guest = createOrUpdateGuest(request.getGuest());

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setGuest(guest);
        booking.setCheckInDateTime(request.getCheckInDate());
        booking.setCheckOutDateTime(request.getCheckOutDate());
        booking.setCurrency("VND");
        booking.setStatus(bookingStateMachine.initialStatus());
        booking.setExpiredAt(LocalDateTime.now().plusMinutes(pendingExpirationMinutes));
        booking.setClientRequestId(clientRequestId);
        booking.setRequestHash(requestHash);

        BigDecimal totalPrice = BigDecimal.ZERO;
        Set<UUID> hotelIds = new HashSet<>();
        for (BookedRoomRequest item : request.getRooms()) {
            RoomType roomType = roomTypeRepository.findById(item.getRoomTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("RoomType", "id", item.getRoomTypeId()));
            if (roomType.getHotel() != null) {
                hotelIds.add(roomType.getHotel().getId());
            }

            if(roomType.getBasePrice() == null || roomType.getBasePrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Room type base price must be greater than zero");
            }

            BookedRoom bookedRoom = buildBookedRoom(roomType, item.getQuantity());
            booking.addBookedRoom(bookedRoom);

            BigDecimal lineTotal = bookedRoom.getUnitPrice()
                    .multiply(BigDecimal.valueOf(nights))
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            totalPrice = totalPrice.add(lineTotal);
        }

        applyCancellationPolicy(booking, request, hotelIds);
        applyDiscount(booking, request, totalPrice);
        booking.setTotalPrice(totalPrice.subtract(booking.getDiscountAmount()));
        Booking savedBooking = bookingRepository.saveAndFlush(booking);
        inventoryService.holdInventory(
                savedBooking,
                savedBooking.getBookedRooms(),
                savedBooking.getCheckInDateTime().toLocalDate(),
                savedBooking.getCheckOutDateTime().toLocalDate(),
                savedBooking.getExpiredAt()
        );
        recordStatusLog(savedBooking, null, savedBooking.getStatus(), currentUser, "Booking created");
        return toBookingResponse(savedBooking);
    }

    private String normalizeClientRequestId(String clientRequestId) {
        if (clientRequestId == null || clientRequestId.isBlank()) {
            throw new BadRequestException("Idempotency-Key header is required");
        }
        String normalized = clientRequestId.trim();
        if (normalized.length() > IDEMPOTENCY_KEY_MAX_LENGTH) {
            throw new BadRequestException("Idempotency-Key must be at most 120 characters");
        }
        return normalized;
    }

    private Optional<BookingResponse> resolveCachedIdempotencyState(
            UUID userId,
            String clientRequestId,
            String requestHash
    ) {
        Optional<BookingIdempotencyState> state = safeFindIdempotencyState(userId, clientRequestId);
        if (state.isEmpty()) {
            return Optional.empty();
        }
        validateRequestHash(state.get().requestHash(), requestHash);
        if (state.get().isProcessing()) {
            throw new ConflictException("Booking request is still processing");
        }
        if (state.get().bookingId() == null) {
            return Optional.empty();
        }
        return transactionTemplate.execute(status -> bookingRepository.findById(state.get().bookingId())
                .map(this::toBookingResponse));
    }

    private Optional<BookingResponse> findExistingBookingResponse(
            UUID userId,
            String clientRequestId,
            String requestHash
    ) {
        return transactionTemplate.execute(status -> bookingRepository
                .findByUser_IdAndClientRequestId(userId, clientRequestId)
                .map(booking -> {
                    validateRequestHash(booking.getRequestHash(), requestHash);
                    return toBookingResponse(booking);
                }));
    }

    private void validateRequestHash(String existingRequestHash, String requestHash) {
        if (!requestHash.equals(existingRequestHash)) {
            throw new ConflictException("Idempotency-Key was reused with a different booking request");
        }
    }

    private Optional<BookingIdempotencyState> safeFindIdempotencyState(UUID userId, String clientRequestId) {
        try {
            return bookingIdempotencyCacheService.find(userId, clientRequestId);
        } catch (RuntimeException ex) {
            logger.warn("Redis idempotency lookup failed; falling back to DB lookup", ex);
            return Optional.empty();
        }
    }

    private boolean safePutProcessingIfAbsent(UUID userId, String clientRequestId, String requestHash) {
        try {
            return bookingIdempotencyCacheService.putProcessingIfAbsent(userId, clientRequestId, requestHash);
        } catch (RuntimeException ex) {
            logger.warn("Redis idempotency PROCESSING write failed; continuing with DB unique constraint", ex);
            return true;
        }
    }

    private void safePutCompleted(UUID userId, String clientRequestId, String requestHash, UUID bookingId) {
        try {
            bookingIdempotencyCacheService.putCompleted(userId, clientRequestId, requestHash, bookingId);
        } catch (RuntimeException ex) {
            logger.warn("Redis idempotency COMPLETED write failed; booking was committed", ex);
        }
    }

    private void safeDeleteIdempotencyState(UUID userId, String clientRequestId) {
        try {
            bookingIdempotencyCacheService.delete(userId, clientRequestId);
        } catch (RuntimeException ex) {
            logger.warn("Redis idempotency cleanup failed", ex);
        }
    }

    @Override
    @Transactional
    public int expirePendingBookings() {
        List<Booking> bookings = bookingRepository
                .findByStatusAndExpiredAtLessThanEqual(BookingStatus.PENDING, LocalDateTime.now());

        for (Booking booking : bookings) {
            bookingStateMachine.transition(booking, BookingStatus.EXPIRED);
            inventoryService.releaseActiveHolds(booking, InventoryHoldStatus.EXPIRED);
            recordStatusLog(booking, BookingStatus.PENDING, BookingStatus.EXPIRED, null, "Booking expired");
        }

        bookingRepository.saveAll(bookings);
        return bookings.size();
    }

    @Override
    @Transactional
    public BookingResponse updateBookingStatus(UUID id, BookingStatusUpdateRequest request, UserPrincipal currentUser) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", id));
        BookingStatus fromStatus = booking.getStatus();
        bookingStateMachine.transition(booking, request.getStatus());
        applyInventoryForStatusTransition(booking, fromStatus, booking.getStatus());
        Booking savedBooking = bookingRepository.save(booking);
        recordStatusLog(
                savedBooking,
                fromStatus,
                savedBooking.getStatus(),
                currentUser,
                resolveNote(request.getReason(), "Booking status updated")
        );
        return toBookingResponse(savedBooking);
    }

    @Override
    public PagedResponse<BookingResponse> getUserBookings(UserPrincipal currentUser, int page, int size) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Booking> bookings = bookingRepository.findByUser(user, pageable);
        List<BookingResponse> responses = bookings.getContent().stream()
                .map(this::toBookingResponse)
                .collect(Collectors.toList());
        return new PagedResponse<>(responses, bookings.getNumber(), bookings.getSize(),
                bookings.getTotalElements(), bookings.getTotalPages(), bookings.isLast());
    }

    @Override
    public BookingResponse getBooking(UUID id, UserPrincipal currentUser) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", id));
        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You don't have permission to view this booking");
        }
        return toBookingResponse(booking);
    }

    private BookingResponse toBookingResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getBookedRooms().stream()
                        .map(this::toBookedRoomResponse)
                        .toList(),
                booking.getCheckInDateTime(),
                booking.getCheckOutDateTime(),
                booking.getTotalPrice(),
                booking.getDiscountAmount(),
                booking.getCancellationFee(),
                booking.getCurrency(),
                booking.getStatus().name(),
                toGuestResponse(booking.getGuest())
        );
    }

    private BookedRoom buildBookedRoom(RoomType roomType, Integer quantity) {
        BookedRoom bookedRoom = new BookedRoom();
        bookedRoom.setRoomType(roomType);
        bookedRoom.setQuantity(quantity);
        bookedRoom.setUnitPrice(roomType.getBasePrice());
        bookedRoom.setRoomTypeNameSnapshot(roomType.getName());
        bookedRoom.setRoomTypeCodeSnapshot(roomType.getCode());
        bookedRoom.setBedTypeSnapshot(roomType.getBedType() == null ? null : roomType.getBedType().name());
        bookedRoom.setMaxOccupancySnapshot(roomType.getMaxOccupancy());
        return bookedRoom;
    }

    private BookedRoomResponse toBookedRoomResponse(BookedRoom bookedRoom) {
        return new BookedRoomResponse(
                bookedRoom.getId(),
                bookedRoom.getRoomType().getId(),
                bookedRoom.getQuantity(),
                bookedRoom.getUnitPrice(),
                bookedRoom.getRoomTypeNameSnapshot(),
                bookedRoom.getRoomTypeCodeSnapshot(),
                bookedRoom.getBedTypeSnapshot(),
                bookedRoom.getMaxOccupancySnapshot()
        );
    }

    private Guest createOrUpdateGuest(BookingGuestRequest request) {
        Optional<Guest> existingByCard = guestRepository.findByIdentifyCardNo(request.getIdentifyCardNo().trim());
        Optional<Guest> existingByEmail = guestRepository.findByEmail(request.getEmail().trim());

        if (existingByCard.isPresent() && existingByEmail.isPresent()
                && !existingByCard.get().getId().equals(existingByEmail.get().getId())) {
            throw new BadRequestException("Guest email is already used by another identity card");
        }

        Guest guest = existingByCard.orElseGet(() -> existingByEmail.orElseGet(Guest::new));
        mapGuestFields(guest, request);
        return guestRepository.save(guest);
    }

    private void mapGuestFields(Guest guest, BookingGuestRequest request) {
        guest.setFirstName(request.getFirstName().trim());
        guest.setLastName(request.getLastName().trim());
        guest.setMiddleName(request.getMiddleName() == null ? null : request.getMiddleName().trim());
        guest.setIdentifyCardNo(request.getIdentifyCardNo().trim());
        guest.setPhoneNumber(request.getPhoneNumber().trim());
        guest.setEmail(request.getEmail().trim());
    }

    @Override
    @Transactional
    public ApiMessageResponse cancelBooking(UUID id, UserPrincipal currentUser, CancelBookingRequest request) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", id));
        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You don't have permission to cancel this booking");
        }
        BookingStatus fromStatus = booking.getStatus();
        bookingStateMachine.transition(booking, BookingStatus.CANCELLED);
        booking.setCancellationFee(calculateCancellationFee(booking));
        applyInventoryForStatusTransition(booking, fromStatus, booking.getStatus());
        Booking savedBooking = bookingRepository.save(booking);
        String note = request.getReason().trim();
        if (savedBooking.getCancellationFee().compareTo(BigDecimal.ZERO) > 0) {
            note = note + ". Cancellation fee: " + savedBooking.getCancellationFee().toPlainString() + " " + savedBooking.getCurrency();
        }
        recordStatusLog(savedBooking, fromStatus, savedBooking.getStatus(), currentUser, note);
        return new ApiMessageResponse(true, "Booking cancelled successfully");
    }

    private void applyDiscount(Booking booking, BookingRequest request, BigDecimal subtotal) {
        booking.setDiscountAmount(BigDecimal.ZERO);
        if (request.getDiscountCode() == null || request.getDiscountCode().isBlank()) {
            return;
        }

        String code = request.getDiscountCode().trim().toUpperCase();
        Discount discount = discountRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("Discount", "code", code));
        validateDiscount(discount, subtotal);

        BigDecimal discountAmount = calculateDiscountAmount(discount, subtotal);
        booking.setDiscount(discount);
        booking.setDiscountCodeSnapshot(discount.getCode());
        booking.setDiscountAmount(discountAmount);
        discount.setUsedCount((discount.getUsedCount() == null ? 0 : discount.getUsedCount()) + 1);
    }

    private void validateDiscount(Discount discount, BigDecimal subtotal) {
        LocalDateTime now = LocalDateTime.now();
        if (!Boolean.TRUE.equals(discount.getIsActive())) {
            throw new BadRequestException("Discount is inactive");
        }
        if (discount.getStartDate().isAfter(now) || discount.getEndDate().isBefore(now)) {
            throw new BadRequestException("Discount is outside its active date range");
        }
        if (discount.getMinOrderValue() != null
                && subtotal.compareTo(BigDecimal.valueOf(discount.getMinOrderValue())) < 0) {
            throw new BadRequestException("Booking total does not meet discount minimum order value");
        }
        if (discount.getMaxOrderValue() != null
                && discount.getMaxOrderValue() > 0
                && subtotal.compareTo(BigDecimal.valueOf(discount.getMaxOrderValue())) > 0) {
            throw new BadRequestException("Booking total exceeds discount maximum order value");
        }
        if (discount.getMaxUsage() != null
                && (discount.getUsedCount() == null ? 0 : discount.getUsedCount()) >= discount.getMaxUsage()) {
            throw new BadRequestException("Discount usage limit has been reached");
        }
    }

    private BigDecimal calculateDiscountAmount(Discount discount, BigDecimal subtotal) {
        BigDecimal amount = discount.getDiscountType() == DiscountType.PERCENTAGE
                ? subtotal.multiply(BigDecimal.valueOf(discount.getDiscountValue()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(discount.getDiscountValue());
        if (amount.compareTo(subtotal) > 0) {
            return subtotal;
        }
        return amount;
    }

    private void applyCancellationPolicy(Booking booking, BookingRequest request, Set<UUID> hotelIds) {
        CancellationPolicy policy = null;
        if (request.getCancellationPolicyId() != null) {
            policy = cancellationPolicyRepository.findById(request.getCancellationPolicyId())
                    .orElseThrow(() -> new ResourceNotFoundException("CancellationPolicy", "id", request.getCancellationPolicyId()));
            validateCancellationPolicyScope(policy, hotelIds);
        } else if (hotelIds.size() == 1) {
            UUID hotelId = hotelIds.iterator().next();
            policy = cancellationPolicyRepository.findByHotel_IdAndIsActiveTrue(hotelId).stream()
                    .findFirst()
                    .orElse(null);
        }
        if (policy == null) {
            return;
        }
        if (!Boolean.TRUE.equals(policy.getIsActive())) {
            throw new BadRequestException("Cancellation policy is inactive");
        }
        booking.setCancellationPolicy(policy);
    }

    private void validateCancellationPolicyScope(CancellationPolicy policy, Set<UUID> hotelIds) {
        if (policy.getHotel() == null) {
            return;
        }
        if (!hotelIds.contains(policy.getHotel().getId())) {
            throw new BadRequestException("Cancellation policy does not belong to the booking hotel");
        }
    }

    private BigDecimal calculateCancellationFee(Booking booking) {
        CancellationPolicy policy = booking.getCancellationPolicy();
        if (policy == null || policy.getPenaltyType() == CancellationPenaltyType.NONE) {
            return BigDecimal.ZERO;
        }

        LocalDateTime freeUntil = booking.getCheckInDateTime().minusHours(policy.getFreeCancellationHours());
        if (LocalDateTime.now().isBefore(freeUntil)) {
            return BigDecimal.ZERO;
        }

        BigDecimal fee = policy.getPenaltyType() == CancellationPenaltyType.PERCENTAGE
                ? booking.getTotalPrice().multiply(BigDecimal.valueOf(policy.getPenaltyValue()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(policy.getPenaltyValue());
        if (fee.compareTo(booking.getTotalPrice()) > 0) {
            return booking.getTotalPrice();
        }
        return fee;
    }

    private com.example.bookingapi.features.booking.dto.response.GuestResponse toGuestResponse(Guest guest) {
        if (guest == null) {
            return null;
        }
        return new com.example.bookingapi.features.booking.dto.response.GuestResponse(
                guest.getId(),
                guest.getFirstName(),
                guest.getLastName(),
                guest.getMiddleName(),
                guest.getIdentifyCardNo(),
                guest.getPhoneNumber(),
                guest.getEmail()
        );
    }

    private void recordStatusLog(
            Booking booking,
            BookingStatus fromStatus,
            BookingStatus toStatus,
            UserPrincipal actor,
            String note
    ) {
        BookingStatusLog log = new BookingStatusLog();
        log.setBooking(booking);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        if (actor == null) {
            log.setPerformedByType(ActorType.SYSTEM);
        } else {
            log.setPerformedBy(actor.getId());
            log.setPerformedByType(actor.getActorType());
        }
        log.setNote(note);
        bookingStatusLogRepository.save(log);
    }

    private String resolveNote(String note, String fallback) {
        if (note == null || note.isBlank()) {
            return fallback;
        }
        return note.trim();
    }

    private void applyInventoryForStatusTransition(
            Booking booking,
            BookingStatus fromStatus,
            BookingStatus toStatus
    ) {
        if (toStatus == BookingStatus.CONFIRMED) {
            inventoryService.consumeActiveHolds(booking);
            return;
        }
        if (toStatus == BookingStatus.EXPIRED) {
            inventoryService.releaseActiveHolds(booking, InventoryHoldStatus.EXPIRED);
            return;
        }
        if (toStatus == BookingStatus.CANCELLED) {
            if (fromStatus == BookingStatus.PENDING) {
                inventoryService.releaseActiveHolds(booking, InventoryHoldStatus.RELEASED);
            }
            if (fromStatus == BookingStatus.CONFIRMED) {
                inventoryService.releaseConsumedHolds(booking);
            }
        }
    }

    @Override
    @Transactional
    public ApiMessageResponse checkInBooking(UUID id, UserPrincipal currentUser) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", id));
        receptionistAccessService.requireCanManageBooking(booking, currentUser);
        BookingStatus fromStatus = booking.getStatus();
        bookingStateMachine.transition(booking, BookingStatus.CHECKED_IN);
        booking.setActualCheckInDate(LocalDateTime.now());
        Booking savedBooking = bookingRepository.save(booking);
        recordStatusLog(savedBooking, fromStatus, savedBooking.getStatus(), currentUser, "Booking checked-in");
        return new ApiMessageResponse(true, "Booking checked-in successfully");
    }

    @Override
    @Transactional
    public ApiMessageResponse checkOutBooking(UUID id, UserPrincipal currentUser) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", id));
        receptionistAccessService.requireCanManageBooking(booking, currentUser);
        BookingStatus fromStatus = booking.getStatus();
        bookingStateMachine.transition(booking, BookingStatus.CHECKED_OUT);
        booking.setActualCheckOutDate(LocalDateTime.now());
        Booking savedBooking = bookingRepository.save(booking);
        recordStatusLog(savedBooking, fromStatus, savedBooking.getStatus(), currentUser, "Booking checked-out");
        return new ApiMessageResponse(true, "Booking checked-out successfully");
    }

    @Override
    @Transactional
    public ApiMessageResponse markNoShow(UUID id, UserPrincipal currentUser) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", id));
        receptionistAccessService.requireCanManageBooking(booking, currentUser);
        BookingStatus fromStatus = booking.getStatus();
        bookingStateMachine.transition(booking, BookingStatus.NO_SHOW);
        Booking savedBooking = bookingRepository.save(booking);
        recordStatusLog(savedBooking, fromStatus, savedBooking.getStatus(), currentUser, "Booking marked as no-show");
        return new ApiMessageResponse(true, "Booking marked as no-show successfully");
    }

}

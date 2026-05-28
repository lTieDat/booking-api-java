package com.example.bookingapi.features.booking.service.impl;

import com.example.bookingapi.common.exception.BadRequestException;
import com.example.bookingapi.common.exception.ResourceNotFoundException;
import com.example.bookingapi.common.exception.UnauthorizedException;
import com.example.bookingapi.features.auth.model.enums.ActorType;
import com.example.bookingapi.features.booking.model.BookedRoom;
import com.example.bookingapi.features.booking.model.Booking;
import com.example.bookingapi.features.booking.model.BookingStatusLog;
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
import com.example.bookingapi.features.booking.service.BookingStateMachine;
import com.example.bookingapi.features.booking.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingStatusLogRepository bookingStatusLogRepository;
    @Autowired private RoomTypeRepository roomTypeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BookingStateMachine bookingStateMachine;

    @Value("${app.booking.pending-expiration-minutes:15}")
    private long pendingExpirationMinutes;

    @Override
    @Transactional
    public BookingResponse createBooking(BookingRequest request, UserPrincipal currentUser) {
        // check if checkin date > checkout date
        if(!request.getCheckInDate().isBefore(request.getCheckOutDate())) {
            throw new BadRequestException("Check-in date must be before check-out date");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());
        booking.setCurrency("VND");
        booking.setStatus(bookingStateMachine.initialStatus());
        booking.setExpiredAt(LocalDateTime.now().plusMinutes(pendingExpirationMinutes));

        BigDecimal totalPrice = BigDecimal.ZERO;
        for (BookedRoomRequest item : request.getRooms()) {
            RoomType roomType = roomTypeRepository.findById(item.getRoomTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("RoomType", "id", item.getRoomTypeId()));

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

        booking.setTotalPrice(totalPrice);
        Booking savedBooking = bookingRepository.save(booking);
        recordStatusLog(savedBooking, null, savedBooking.getStatus(), currentUser, "Booking created");
        return toBookingResponse(savedBooking);
    }

    @Override
    @Transactional
    public int expirePendingBookings() {
        List<Booking> bookings = bookingRepository
                .findByStatusAndExpiredAtLessThanEqual(BookingStatus.PENDING, LocalDateTime.now());

        for (Booking booking : bookings) {
            bookingStateMachine.transition(booking, BookingStatus.EXPIRED);
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
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getTotalPrice(),
                booking.getCurrency(),
                booking.getStatus().name()
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
        Booking savedBooking = bookingRepository.save(booking);
        recordStatusLog(savedBooking, fromStatus, savedBooking.getStatus(), currentUser, request.getReason().trim());
        return new ApiMessageResponse(true, "Booking cancelled successfully");
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

    @Override
    @Transactional
    public ApiMessageResponse checkInBooking(UUID id, UserPrincipal currentUser) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", id));
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
        BookingStatus fromStatus = booking.getStatus();
        bookingStateMachine.transition(booking, BookingStatus.NO_SHOW);
        Booking savedBooking = bookingRepository.save(booking);
        recordStatusLog(savedBooking, fromStatus, savedBooking.getStatus(), currentUser, "Booking marked as no-show");
        return new ApiMessageResponse(true, "Booking marked as no-show successfully");
    }

}

package com.example.bookingapi.features.receptionist.service.impl;

import com.example.bookingapi.common.exception.ResourceNotFoundException;
import com.example.bookingapi.common.response.ApiMessageResponse;
import com.example.bookingapi.common.response.PagedResponse;
import com.example.bookingapi.common.security.UserPrincipal;
import com.example.bookingapi.features.auth.model.Role;
import com.example.bookingapi.features.auth.model.enums.RoleName;
import com.example.bookingapi.features.auth.repository.RoleRepository;
import com.example.bookingapi.features.booking.dto.response.BookedRoomResponse;
import com.example.bookingapi.features.booking.dto.response.BookingResponse;
import com.example.bookingapi.features.booking.dto.response.GuestResponse;
import com.example.bookingapi.features.booking.model.BookedRoom;
import com.example.bookingapi.features.booking.model.Booking;
import com.example.bookingapi.features.booking.model.enums.BookingStatus;
import com.example.bookingapi.features.booking.repository.BookingRepository;
import com.example.bookingapi.features.hotel.dto.response.HotelImageResponse;
import com.example.bookingapi.features.hotel.dto.response.LocationResponse;
import com.example.bookingapi.features.hotel.model.Hotel;
import com.example.bookingapi.features.hotel.model.HotelImage;
import com.example.bookingapi.features.hotel.model.Location;
import com.example.bookingapi.features.hotel.model.enums.HotelImageType;
import com.example.bookingapi.features.hotel.repository.HotelRepository;
import com.example.bookingapi.features.receptionist.dto.request.ReceptionistAssignmentRequest;
import com.example.bookingapi.features.receptionist.dto.response.ReceptionistAssignmentResponse;
import com.example.bookingapi.features.receptionist.dto.response.ReceptionistHotelResponse;
import com.example.bookingapi.features.receptionist.model.ReceptionistAssignment;
import com.example.bookingapi.features.receptionist.repository.ReceptionistAssignmentRepository;
import com.example.bookingapi.features.receptionist.service.ReceptionistAccessService;
import com.example.bookingapi.features.receptionist.service.ReceptionistService;
import com.example.bookingapi.features.user.model.User;
import com.example.bookingapi.features.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReceptionistServiceImpl implements ReceptionistService {

    private final ReceptionistAssignmentRepository receptionistAssignmentRepository;
    private final ReceptionistAccessService receptionistAccessService;
    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;
    private final RoleRepository roleRepository;
    private final BookingRepository bookingRepository;

    public ReceptionistServiceImpl(
            ReceptionistAssignmentRepository receptionistAssignmentRepository,
            ReceptionistAccessService receptionistAccessService,
            UserRepository userRepository,
            HotelRepository hotelRepository,
            RoleRepository roleRepository,
            BookingRepository bookingRepository
    ) {
        this.receptionistAssignmentRepository = receptionistAssignmentRepository;
        this.receptionistAccessService = receptionistAccessService;
        this.userRepository = userRepository;
        this.hotelRepository = hotelRepository;
        this.roleRepository = roleRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    @Transactional
    public ReceptionistAssignmentResponse assignReceptionist(
            ReceptionistAssignmentRequest request,
            UserPrincipal currentUser
    ) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));
        Hotel hotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", request.getHotelId()));

        grantReceptionistRoleIfMissing(user);

        ReceptionistAssignment assignment = receptionistAssignmentRepository
                .findByUser_IdAndHotel_Id(user.getId(), hotel.getId())
                .orElseGet(ReceptionistAssignment::new);
        assignment.setUser(user);
        assignment.setHotel(hotel);
        assignment.setIsActive(request.getActive() == null || Boolean.TRUE.equals(request.getActive()));

        return toAssignmentResponse(receptionistAssignmentRepository.save(assignment));
    }

    @Override
    public PagedResponse<ReceptionistAssignmentResponse> getAssignments(
            UUID userId,
            UUID hotelId,
            Boolean active,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<ReceptionistAssignment> assignments = findAssignments(userId, hotelId, active, pageable);
        List<ReceptionistAssignmentResponse> content = assignments.getContent().stream()
                .map(this::toAssignmentResponse)
                .toList();
        return new PagedResponse<>(
                content,
                assignments.getNumber(),
                assignments.getSize(),
                assignments.getTotalElements(),
                assignments.getTotalPages(),
                assignments.isLast()
        );
    }

    @Override
    @Transactional
    public ApiMessageResponse deactivateAssignment(UUID id) {
        ReceptionistAssignment assignment = receptionistAssignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReceptionistAssignment", "id", id));
        assignment.setIsActive(false);
        receptionistAssignmentRepository.save(assignment);
        return new ApiMessageResponse(true, "Receptionist assignment deactivated successfully");
    }

    @Override
    public List<ReceptionistHotelResponse> getMyHotels(UserPrincipal currentUser) {
        if (!hasRole(currentUser, "ROLE_RECEPTIONIST")) {
            throw new AccessDeniedException("Only receptionist can view assigned hotels");
        }
        return receptionistAssignmentRepository.findByUser_IdAndIsActiveTrue(currentUser.getId()).stream()
                .map(assignment -> toHotelResponse(assignment.getHotel()))
                .toList();
    }

    @Override
    public PagedResponse<BookingResponse> getHotelBookings(
            UUID hotelId,
            BookingStatus status,
            int page,
            int size,
            UserPrincipal currentUser
    ) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", hotelId));
        if (!hasRole(currentUser, "ROLE_ADMIN")
                && !receptionistAssignmentRepository.existsByUser_IdAndHotel_IdAndIsActiveTrue(currentUser.getId(), hotel.getId())) {
            throw new AccessDeniedException("Receptionist is not assigned to this hotel");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Booking> bookings = status == null
                ? bookingRepository.findDistinctByHotelId(hotelId, pageable)
                : bookingRepository.findDistinctByHotelIdAndStatus(hotelId, status, pageable);
        List<BookingResponse> content = bookings.getContent().stream()
                .map(booking -> {
                    receptionistAccessService.requireCanManageBooking(booking, currentUser);
                    return toBookingResponse(booking);
                })
                .toList();
        return new PagedResponse<>(
                content,
                bookings.getNumber(),
                bookings.getSize(),
                bookings.getTotalElements(),
                bookings.getTotalPages(),
                bookings.isLast()
        );
    }

    private void grantReceptionistRoleIfMissing(User user) {
        boolean alreadyReceptionist = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_RECEPTIONIST);
        if (alreadyReceptionist) {
            return;
        }

        Role receptionistRole = roleRepository.findByName(RoleName.ROLE_RECEPTIONIST)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", RoleName.ROLE_RECEPTIONIST));
        user.getRoles().add(receptionistRole);
    }

    private Page<ReceptionistAssignment> findAssignments(
            UUID userId,
            UUID hotelId,
            Boolean active,
            Pageable pageable
    ) {
        if (userId != null && hotelId != null && active != null) {
            return receptionistAssignmentRepository.findByUser_IdAndHotel_IdAndIsActive(userId, hotelId, active, pageable);
        }
        if (userId != null && hotelId != null) {
            return receptionistAssignmentRepository.findByUser_IdAndHotel_Id(userId, hotelId, pageable);
        }
        if (userId != null && active != null) {
            return receptionistAssignmentRepository.findByUser_IdAndIsActive(userId, active, pageable);
        }
        if (hotelId != null && active != null) {
            return receptionistAssignmentRepository.findByHotel_IdAndIsActive(hotelId, active, pageable);
        }
        if (userId != null) {
            return receptionistAssignmentRepository.findByUser_Id(userId, pageable);
        }
        if (hotelId != null) {
            return receptionistAssignmentRepository.findByHotel_Id(hotelId, pageable);
        }
        if (active != null) {
            return receptionistAssignmentRepository.findByIsActive(active, pageable);
        }
        return receptionistAssignmentRepository.findAll(pageable);
    }

    private ReceptionistAssignmentResponse toAssignmentResponse(ReceptionistAssignment assignment) {
        return new ReceptionistAssignmentResponse(
                assignment.getId(),
                assignment.getUser().getId(),
                assignment.getUser().getEmail(),
                assignment.getUser().getName(),
                assignment.getHotel().getId(),
                assignment.getHotel().getName(),
                assignment.getIsActive()
        );
    }

    private ReceptionistHotelResponse toHotelResponse(Hotel hotel) {
        return new ReceptionistHotelResponse(
                hotel.getId(),
                hotel.getName(),
                hotel.getDescription(),
                toLocationResponse(hotel.getLocation()),
                toPreviewImageResponse(hotel)
        );
    }

    private LocationResponse toLocationResponse(Location location) {
        if (location == null) {
            return null;
        }
        return new LocationResponse(
                location.getCountry(),
                location.getCity(),
                location.getProvince(),
                location.getDistrict(),
                location.getDetail(),
                location.getLatitude(),
                location.getLongitude()
        );
    }

    private HotelImageResponse toPreviewImageResponse(Hotel hotel) {
        return hotel.getImages().stream()
                .filter(image -> image.getImageType() == HotelImageType.PREVIEW)
                .findFirst()
                .map(this::toImageResponse)
                .orElse(null);
    }

    private HotelImageResponse toImageResponse(HotelImage image) {
        return new HotelImageResponse(
                image.getUrl(),
                image.getBucket(),
                image.getObjectKey(),
                image.getContentType(),
                image.getSizeBytes(),
                image.getAltText(),
                image.getImageType()
        );
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
                booking.getGuest() == null ? null : new GuestResponse(
                        booking.getGuest().getId(),
                        booking.getGuest().getFirstName(),
                        booking.getGuest().getLastName(),
                        booking.getGuest().getMiddleName(),
                        booking.getGuest().getIdentifyCardNo(),
                        booking.getGuest().getPhoneNumber(),
                        booking.getGuest().getEmail()
                )
        );
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

    private boolean hasRole(UserPrincipal currentUser, String roleName) {
        return currentUser.getAuthorities().stream()
                .anyMatch(authority -> roleName.equals(authority.getAuthority()));
    }
}

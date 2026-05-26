package com.example.bookingapi.service.impl;

import com.example.bookingapi.exception.BadRequestException;
import com.example.bookingapi.exception.ResourceNotFoundException;
import com.example.bookingapi.exception.UnauthorizedException;
import com.example.bookingapi.model.Booking;
import com.example.bookingapi.model.Room;
import com.example.bookingapi.model.User;
import com.example.bookingapi.payload.request.BookingRequest;
import com.example.bookingapi.payload.response.ApiResponse;
import com.example.bookingapi.payload.response.BookingResponse;
import com.example.bookingapi.payload.response.PagedResponse;
import com.example.bookingapi.repository.BookingRepository;
import com.example.bookingapi.repository.RoomRepository;
import com.example.bookingapi.repository.UserRepository;
import com.example.bookingapi.security.UserPrincipal;
import com.example.bookingapi.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    @Autowired private BookingRepository bookingRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private UserRepository userRepository;

    @Override
    @Transactional
    public BookingResponse createBooking(BookingRequest request, UserPrincipal currentUser) {
        // check if checkin date > checkout date
        if(!request.getCheckInDate().isBefore(request.getCheckOutDate())) {
            throw new BadRequestException("Check-in date must be before check-out date");
        }
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", request.getRoomId()));

        //check if room price is not real number
        if(room.getPricePerNight() == null || room.getPricePerNight().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Room price per night must be greater than zero");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        BigDecimal totalPrice = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setRoom(room);
        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());
        booking.setTotalPrice(totalPrice);
        booking.setStatus("CONFIRMED");
        return toBookingResponse(bookingRepository.save(booking));
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
                booking.getRoom().getId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getTotalPrice(),
                booking.getStatus()
        );
    }

    @Override
    @Transactional
    public ApiResponse cancelBooking(UUID id, UserPrincipal currentUser) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", id));
        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You don't have permission to cancel this booking");
        }
        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);
        return new ApiResponse(true, "Booking cancelled successfully");
    }
}

package com.example.bookingapi.service;

import com.example.bookingapi.model.Booking;
import com.example.bookingapi.payload.request.BookingRequest;
import com.example.bookingapi.payload.response.ApiResponse;
import com.example.bookingapi.payload.response.PagedResponse;
import com.example.bookingapi.security.UserPrincipal;

import java.util.UUID;

public interface BookingService {
    Booking createBooking(BookingRequest bookingRequest, UserPrincipal currentUser);
    PagedResponse<Booking> getUserBookings(UserPrincipal currentUser, int page, int size);
    Booking getBooking(UUID id, UserPrincipal currentUser);
    ApiResponse cancelBooking(UUID id, UserPrincipal currentUser);
}

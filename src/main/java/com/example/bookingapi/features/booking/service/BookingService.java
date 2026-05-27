package com.example.bookingapi.features.booking.service;

import com.example.bookingapi.features.booking.dto.request.BookingRequest;
import com.example.bookingapi.common.response.ApiMessageResponse;
import com.example.bookingapi.features.booking.dto.response.BookingResponse;
import com.example.bookingapi.common.response.PagedResponse;
import com.example.bookingapi.common.security.UserPrincipal;

import java.util.UUID;

public interface BookingService {
    BookingResponse createBooking(BookingRequest bookingRequest, UserPrincipal currentUser);
    PagedResponse<BookingResponse> getUserBookings(UserPrincipal currentUser, int page, int size);
    BookingResponse getBooking(UUID id, UserPrincipal currentUser);
    ApiMessageResponse cancelBooking(UUID id, UserPrincipal currentUser);
}

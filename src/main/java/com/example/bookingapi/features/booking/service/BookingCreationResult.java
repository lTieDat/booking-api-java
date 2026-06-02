package com.example.bookingapi.features.booking.service;

import com.example.bookingapi.features.booking.dto.response.BookingResponse;

public record BookingCreationResult(
        BookingResponse response,
        boolean created
) {

    public static BookingCreationResult created(BookingResponse response) {
        return new BookingCreationResult(response, true);
    }

    public static BookingCreationResult reused(BookingResponse response) {
        return new BookingCreationResult(response, false);
    }
}

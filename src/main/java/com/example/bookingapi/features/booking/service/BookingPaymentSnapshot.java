package com.example.bookingapi.features.booking.service;

import com.example.bookingapi.features.booking.model.Booking;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record BookingPaymentSnapshot(
        Booking booking,
        UUID bookingId,
        BigDecimal amount,
        String currency,
        LocalDateTime expiresAt,
        List<BookingPaymentItem> items
) {
}

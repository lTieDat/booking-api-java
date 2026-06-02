package com.example.bookingapi.features.booking.service;

import java.math.BigDecimal;

public record BookingPaymentItem(
        String name,
        Integer quantity,
        BigDecimal unitAmount
) {
}

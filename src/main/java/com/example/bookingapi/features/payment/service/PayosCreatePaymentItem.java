package com.example.bookingapi.features.payment.service;

public record PayosCreatePaymentItem(
        String name,
        Integer quantity,
        Long unitPriceMinor
) {
}

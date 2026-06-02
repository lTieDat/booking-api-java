package com.example.bookingapi.features.payment.service;

import java.time.LocalDateTime;
import java.util.List;

public record PayosCreatePaymentCommand(
        Long orderCode,
        Long amountMinor,
        String description,
        String returnUrl,
        String cancelUrl,
        LocalDateTime expiresAt,
        List<PayosCreatePaymentItem> items
) {
}

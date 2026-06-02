package com.example.bookingapi.features.payment.service;

import java.util.List;

public record PayosPaymentLinkResult(
        Long orderCode,
        Long amountMinor,
        Long amountPaid,
        Long amountRemaining,
        String paymentLinkId,
        String status,
        List<PayosPaymentLinkTransaction> transactions
) {
}

package com.example.bookingapi.features.payment.service;

public record PayosWebhookVerificationResult(
        Long orderCode,
        Long amountMinor,
        String description,
        String reference,
        String transactionDateTime,
        String currency,
        String paymentLinkId,
        String code,
        String desc
) {
}

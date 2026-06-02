package com.example.bookingapi.features.payment.service;

public record PayosPaymentLinkTransaction(
        String reference,
        Long amountMinor,
        String description,
        String transactionDateTime
) {
}

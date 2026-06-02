package com.example.bookingapi.features.payment.service;

public record PayosCreatePaymentResult(
        Long orderCode,
        String paymentLinkId,
        String checkoutUrl,
        String qrCode,
        String providerStatus,
        Long expiredAt
) {
}

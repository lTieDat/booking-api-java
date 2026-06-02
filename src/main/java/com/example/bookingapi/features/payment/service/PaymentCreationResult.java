package com.example.bookingapi.features.payment.service;

import com.example.bookingapi.features.payment.dto.response.PaymentResponse;

public record PaymentCreationResult(
        boolean created,
        PaymentResponse response
) {
    public static PaymentCreationResult created(PaymentResponse response) {
        return new PaymentCreationResult(true, response);
    }

    public static PaymentCreationResult reused(PaymentResponse response) {
        return new PaymentCreationResult(false, response);
    }
}

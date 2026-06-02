package com.example.bookingapi.features.payment.service;

import com.example.bookingapi.common.security.UserPrincipal;
import com.example.bookingapi.features.payment.dto.response.PaymentResponse;

import java.util.UUID;

public interface PaymentService {
    PaymentCreationResult createPayosPayment(UUID bookingId, UserPrincipal currentUser);
    PaymentResponse getPayment(UUID paymentId, UserPrincipal currentUser);
}

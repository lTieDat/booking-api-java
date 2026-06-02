package com.example.bookingapi.features.payment.service;

import com.example.bookingapi.common.security.UserPrincipal;
import com.example.bookingapi.features.payment.dto.request.RefundRequest;
import com.example.bookingapi.features.payment.dto.response.RefundResponse;

import java.util.UUID;

public interface RefundService {
    RefundResponse requestManualRefund(UUID paymentId, RefundRequest request, UserPrincipal currentUser);
}

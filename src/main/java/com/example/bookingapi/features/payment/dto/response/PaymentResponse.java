package com.example.bookingapi.features.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class PaymentResponse {
    private UUID paymentId;
    private UUID bookingId;
    private String provider;
    private String status;
    private Long amountMinor;
    private String currency;
    private String orderCode;
    private String paymentLinkId;
    private String checkoutUrl;
    private String qrCode;
    private LocalDateTime expiresAt;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
}

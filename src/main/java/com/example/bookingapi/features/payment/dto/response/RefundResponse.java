package com.example.bookingapi.features.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class RefundResponse {
    private UUID refundId;
    private UUID paymentId;
    private UUID bookingId;
    private Long amountMinor;
    private String currency;
    private String status;
    private String reason;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
}

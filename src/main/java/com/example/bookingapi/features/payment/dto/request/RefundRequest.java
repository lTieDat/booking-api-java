package com.example.bookingapi.features.payment.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefundRequest {
    @Min(value = 1, message = "Refund amount must be greater than zero")
    private Long amountMinor;

    @NotBlank(message = "Refund reason is required")
    @Size(max = 300, message = "Refund reason must be at most 300 characters")
    private String reason;
}

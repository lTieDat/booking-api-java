package com.example.bookingapi.features.invoice.dto.request;

import com.example.bookingapi.features.invoice.model.enums.TaxApplyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class TaxConfigRequest {
    private UUID hotelId;

    @NotBlank(message = "Tax name is required")
    @Size(max = 100, message = "Tax name must be at most 100 characters")
    private String name;

    @NotNull(message = "Tax apply type is required")
    private TaxApplyType applyType;

    @PositiveOrZero(message = "Tax rate must be non-negative")
    private BigDecimal rate;

    @PositiveOrZero(message = "Tax amount must be non-negative")
    private Long amountMinor;

    private Boolean inclusive = false;
    private Boolean active = true;
}

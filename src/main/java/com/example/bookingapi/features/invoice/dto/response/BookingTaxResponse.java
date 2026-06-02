package com.example.bookingapi.features.invoice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class BookingTaxResponse {
    private UUID id;
    private UUID taxConfigId;
    private String taxName;
    private String applyType;
    private BigDecimal rate;
    private Long amountMinor;
    private Boolean inclusive;
}

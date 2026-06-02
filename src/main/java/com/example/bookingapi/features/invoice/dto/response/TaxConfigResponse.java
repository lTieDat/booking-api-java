package com.example.bookingapi.features.invoice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class TaxConfigResponse {
    private UUID id;
    private UUID hotelId;
    private String name;
    private String applyType;
    private BigDecimal rate;
    private Long amountMinor;
    private Boolean inclusive;
    private Boolean active;
}

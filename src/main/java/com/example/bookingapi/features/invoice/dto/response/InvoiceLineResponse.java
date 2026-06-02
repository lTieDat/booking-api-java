package com.example.bookingapi.features.invoice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class InvoiceLineResponse {
    private UUID id;
    private String lineType;
    private String description;
    private Integer quantity;
    private Long unitMinor;
    private Long totalMinor;
    private String metadata;
}

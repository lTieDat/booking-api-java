package com.example.bookingapi.features.invoice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class InvoiceResponse {
    private UUID id;
    private UUID bookingId;
    private UUID paymentId;
    private String invoiceNo;
    private String status;
    private Long subtotalMinor;
    private Long discountMinor;
    private Long taxMinor;
    private Long totalMinor;
    private String currency;
    private LocalDateTime issuedAt;
    private LocalDateTime paidAt;
    private List<InvoiceLineResponse> lines;
    private List<BookingTaxResponse> taxes;
}

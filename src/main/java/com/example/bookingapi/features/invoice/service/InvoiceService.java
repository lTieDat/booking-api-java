package com.example.bookingapi.features.invoice.service;

import com.example.bookingapi.common.security.UserPrincipal;
import com.example.bookingapi.features.invoice.dto.request.TaxConfigRequest;
import com.example.bookingapi.features.invoice.dto.response.InvoiceResponse;
import com.example.bookingapi.features.invoice.dto.response.TaxConfigResponse;
import com.example.bookingapi.features.payment.model.Payment;

import java.util.List;
import java.util.UUID;

public interface InvoiceService {
    InvoiceResponse generatePaidInvoiceForPayment(Payment payment);
    InvoiceResponse getInvoice(UUID invoiceId, UserPrincipal currentUser);
    InvoiceResponse getInvoiceByPayment(UUID paymentId, UserPrincipal currentUser);
    List<TaxConfigResponse> getTaxConfigs();
    TaxConfigResponse createTaxConfig(TaxConfigRequest request);
    TaxConfigResponse updateTaxConfig(UUID id, TaxConfigRequest request);
}

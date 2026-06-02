package com.example.bookingapi.features.invoice.controller;

import com.example.bookingapi.common.openapi.CommonApiResponses;
import com.example.bookingapi.common.security.CurrentUser;
import com.example.bookingapi.common.security.UserPrincipal;
import com.example.bookingapi.features.invoice.dto.request.TaxConfigRequest;
import com.example.bookingapi.features.invoice.dto.response.InvoiceResponse;
import com.example.bookingapi.features.invoice.dto.response.TaxConfigResponse;
import com.example.bookingapi.features.invoice.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Invoices", description = "Invoice and tax configuration endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping("/invoices/{invoiceId}")
    @Operation(summary = "Get invoice by id", description = "Return invoice details for owner or admin.")
    @ApiResponse(responseCode = "200", description = "Invoice returned successfully.",
            content = @Content(schema = @Schema(implementation = InvoiceResponse.class)))
    @CommonApiResponses
    public ResponseEntity<InvoiceResponse> getInvoice(
            @PathVariable UUID invoiceId,
            @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(invoiceService.getInvoice(invoiceId, currentUser));
    }

    @GetMapping("/payments/{paymentId}/invoice")
    @Operation(summary = "Get invoice by payment", description = "Return invoice generated for a payment.")
    @ApiResponse(responseCode = "200", description = "Invoice returned successfully.",
            content = @Content(schema = @Schema(implementation = InvoiceResponse.class)))
    @CommonApiResponses
    public ResponseEntity<InvoiceResponse> getInvoiceByPayment(
            @PathVariable UUID paymentId,
            @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(invoiceService.getInvoiceByPayment(paymentId, currentUser));
    }

    @GetMapping("/tax-configs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List tax configs", description = "Admin only.")
    @CommonApiResponses
    public ResponseEntity<List<TaxConfigResponse>> getTaxConfigs() {
        return ResponseEntity.ok(invoiceService.getTaxConfigs());
    }

    @PostMapping("/tax-configs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create tax config", description = "Admin only.")
    @ApiResponse(responseCode = "201", description = "Tax config created successfully.",
            content = @Content(schema = @Schema(implementation = TaxConfigResponse.class)))
    @CommonApiResponses
    public ResponseEntity<TaxConfigResponse> createTaxConfig(@Valid @RequestBody TaxConfigRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.createTaxConfig(request));
    }

    @PutMapping("/tax-configs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update tax config", description = "Admin only.")
    @ApiResponse(responseCode = "200", description = "Tax config updated successfully.",
            content = @Content(schema = @Schema(implementation = TaxConfigResponse.class)))
    @CommonApiResponses
    public ResponseEntity<TaxConfigResponse> updateTaxConfig(
            @PathVariable UUID id,
            @Valid @RequestBody TaxConfigRequest request
    ) {
        return ResponseEntity.ok(invoiceService.updateTaxConfig(id, request));
    }
}

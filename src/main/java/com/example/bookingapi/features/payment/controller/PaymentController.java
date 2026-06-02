package com.example.bookingapi.features.payment.controller;

import com.example.bookingapi.common.openapi.CommonApiResponses;
import com.example.bookingapi.common.security.CurrentUser;
import com.example.bookingapi.common.security.UserPrincipal;
import com.example.bookingapi.features.payment.dto.request.RefundRequest;
import com.example.bookingapi.features.payment.dto.response.PaymentResponse;
import com.example.bookingapi.features.payment.dto.response.RefundResponse;
import com.example.bookingapi.features.payment.service.PaymentCreationResult;
import com.example.bookingapi.features.payment.service.PaymentService;
import com.example.bookingapi.features.payment.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Payments", description = "Payment management endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;
    private final RefundService refundService;

    public PaymentController(PaymentService paymentService, RefundService refundService) {
        this.paymentService = paymentService;
        this.refundService = refundService;
    }

    @PostMapping("/bookings/{bookingId}/payments/payos")
    @Operation(
            summary = "Create payOS payment link",
            description = "Create or reuse an active payOS payment link for a pending booking owned by the current user."
    )
    @ApiResponse(responseCode = "201", description = "New payOS payment link created.",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class)))
    @ApiResponse(responseCode = "200", description = "Existing active payOS payment link returned.",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class)))
    @ApiResponse(responseCode = "409", description = "Payment link creation is already in progress.")
    @CommonApiResponses
    public ResponseEntity<PaymentResponse> createPayosPayment(
            @PathVariable UUID bookingId,
            @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser
    ) {
        PaymentCreationResult result = paymentService.createPayosPayment(bookingId, currentUser);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.response());
    }

    @GetMapping("/payments/{paymentId}")
    @Operation(
            summary = "Get payment by id",
            description = "Return a payment that belongs to the current authenticated user's booking."
    )
    @ApiResponse(responseCode = "200", description = "Payment returned successfully.",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class)))
    @CommonApiResponses
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable UUID paymentId,
            @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(paymentService.getPayment(paymentId, currentUser));
    }

    @PostMapping("/payments/{paymentId}/refunds")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Create manual refund",
            description = "Create a manual refund record for a paid payment. This does not call a payOS refund API."
    )
    @ApiResponse(responseCode = "201", description = "Manual refund created successfully.",
            content = @Content(schema = @Schema(implementation = RefundResponse.class)))
    @CommonApiResponses
    public ResponseEntity<RefundResponse> createManualRefund(
            @PathVariable UUID paymentId,
            @Valid @org.springframework.web.bind.annotation.RequestBody RefundRequest request,
            @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(refundService.requestManualRefund(paymentId, request, currentUser));
    }
}

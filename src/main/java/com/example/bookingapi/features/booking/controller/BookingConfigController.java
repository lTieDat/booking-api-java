package com.example.bookingapi.features.booking.controller;

import com.example.bookingapi.common.openapi.CommonApiResponses;
import com.example.bookingapi.common.response.ApiMessageResponse;
import com.example.bookingapi.common.response.PagedResponse;
import com.example.bookingapi.common.util.AppConstants;
import com.example.bookingapi.features.booking.dto.request.CancellationPolicyRequest;
import com.example.bookingapi.features.booking.dto.request.DiscountRequest;
import com.example.bookingapi.features.booking.dto.response.CancellationPolicyResponse;
import com.example.bookingapi.features.booking.dto.response.DiscountResponse;
import com.example.bookingapi.features.booking.service.BookingConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/booking-config")
@Tag(name = "Booking Config", description = "Admin booking policy, discount, and cancellation configuration.")
@SecurityRequirement(name = "bearerAuth")
public class BookingConfigController {

    private final BookingConfigService bookingConfigService;

    public BookingConfigController(BookingConfigService bookingConfigService) {
        this.bookingConfigService = bookingConfigService;
    }

    @PostMapping("/discounts")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create discount", description = "Create a promotion/discount code. Admin only.")
    @ApiResponse(responseCode = "201", description = "Discount created successfully.",
            content = @Content(schema = @Schema(implementation = DiscountResponse.class)))
    @CommonApiResponses
    public ResponseEntity<DiscountResponse> createDiscount(@Valid @RequestBody DiscountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingConfigService.createDiscount(request));
    }

    @PutMapping("/discounts/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update discount", description = "Update a promotion/discount code. Admin only.")
    @CommonApiResponses
    public ResponseEntity<DiscountResponse> updateDiscount(
            @PathVariable UUID id,
            @Valid @RequestBody DiscountRequest request) {
        return ResponseEntity.ok(bookingConfigService.updateDiscount(id, request));
    }

    @GetMapping("/discounts")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List discounts", description = "List promotion/discount codes. Admin only.")
    @CommonApiResponses
    public ResponseEntity<PagedResponse<DiscountResponse>> getDiscounts(
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        return ResponseEntity.ok(bookingConfigService.getDiscounts(active, page, size));
    }

    @DeleteMapping("/discounts/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate discount", description = "Deactivate a promotion/discount code. Admin only.")
    @CommonApiResponses
    public ResponseEntity<ApiMessageResponse> deactivateDiscount(@PathVariable UUID id) {
        return ResponseEntity.ok(bookingConfigService.deactivateDiscount(id));
    }

    @PostMapping("/cancellation-policies")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create cancellation policy", description = "Create a cancellation policy. Admin only.")
    @ApiResponse(responseCode = "201", description = "Cancellation policy created successfully.",
            content = @Content(schema = @Schema(implementation = CancellationPolicyResponse.class)))
    @CommonApiResponses
    public ResponseEntity<CancellationPolicyResponse> createCancellationPolicy(
            @Valid @RequestBody CancellationPolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingConfigService.createCancellationPolicy(request));
    }

    @PutMapping("/cancellation-policies/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update cancellation policy", description = "Update a cancellation policy. Admin only.")
    @CommonApiResponses
    public ResponseEntity<CancellationPolicyResponse> updateCancellationPolicy(
            @PathVariable UUID id,
            @Valid @RequestBody CancellationPolicyRequest request) {
        return ResponseEntity.ok(bookingConfigService.updateCancellationPolicy(id, request));
    }

    @GetMapping("/cancellation-policies")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List cancellation policies", description = "List cancellation policies. Admin only.")
    @CommonApiResponses
    public ResponseEntity<PagedResponse<CancellationPolicyResponse>> getCancellationPolicies(
            @RequestParam(required = false) UUID hotelId,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        return ResponseEntity.ok(bookingConfigService.getCancellationPolicies(hotelId, page, size));
    }

    @DeleteMapping("/cancellation-policies/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate cancellation policy", description = "Deactivate a cancellation policy. Admin only.")
    @CommonApiResponses
    public ResponseEntity<ApiMessageResponse> deactivateCancellationPolicy(@PathVariable UUID id) {
        return ResponseEntity.ok(bookingConfigService.deactivateCancellationPolicy(id));
    }
}

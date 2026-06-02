package com.example.bookingapi.features.booking.controller;

import com.example.bookingapi.common.openapi.CommonApiResponses;
import com.example.bookingapi.features.booking.dto.request.BookingRequest;
import com.example.bookingapi.features.booking.dto.request.BookingStatusUpdateRequest;
import com.example.bookingapi.features.booking.dto.request.CancelBookingRequest;
import com.example.bookingapi.features.booking.dto.response.BookingResponse;
import com.example.bookingapi.common.response.ApiMessageResponse;
import com.example.bookingapi.common.response.PagedResponse;
import com.example.bookingapi.common.security.CurrentUser;
import com.example.bookingapi.common.security.UserPrincipal;
import com.example.bookingapi.features.booking.service.BookingCreationResult;
import com.example.bookingapi.features.booking.service.BookingService;
import com.example.bookingapi.common.util.AppConstants;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@Tag(
        name = "Bookings",
        description = "Booking management endpoints. Front-desk local/dev account: reception@booking.local / admin123."
)
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @PostMapping
    @Operation(
            summary = "Create booking",
            description = "Create a new booking for the current authenticated user. "
                    + "Requires `Idempotency-Key` header. Retrying with the same key and identical payload returns "
                    + "the existing booking instead of creating a duplicate."
    )
    @ApiResponse(responseCode = "201", description = "Booking created successfully.",
            content = @Content(schema = @Schema(implementation = BookingResponse.class)))
    @ApiResponse(responseCode = "200", description = "Idempotent retry returned an existing booking.",
            content = @Content(schema = @Schema(implementation = BookingResponse.class)))
    @ApiResponse(responseCode = "409", description = "Idempotency key is still processing or was reused with a different payload.")
    @ApiResponse(responseCode = "423", description = "Inventory is locked by another booking request. Retry after the response Retry-After header.")
    @CommonApiResponses
    public ResponseEntity<BookingResponse> createBooking(
                                                  @Valid @RequestBody BookingRequest bookingRequest,
                                                  @Parameter(hidden = true)
                                                  @CurrentUser UserPrincipal currentUser,
                                                  @Parameter(
                                                          description = "Client-generated idempotency key for safe retries. "
                                                                  + "Use a unique UUID/ULID per booking attempt and reuse it only for retries of the exact same payload.",
                                                          required = true,
                                                          example = "01JZ7Q3M2AZR9V6DS6E8Q8XK7A"
                                                  )
                                                  @RequestHeader("Idempotency-Key") String clientRequestId) {
        BookingCreationResult result = bookingService.createBooking(bookingRequest, currentUser, clientRequestId);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.response());
    }

    @GetMapping("/me")
    @Operation(summary = "Get my bookings", description = "Return paginated bookings of the current authenticated user.")
    @ApiResponse(responseCode = "200", description = "Current user's bookings returned successfully.",
            content = @Content(schema = @Schema(implementation = PagedResponse.class)))
    @CommonApiResponses
    public ResponseEntity<PagedResponse<BookingResponse>> getMyBookings(
            @Parameter(hidden = true)
            @CurrentUser UserPrincipal currentUser,
            @Parameter(description = "Page index starting from 0")
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        return ResponseEntity.ok(bookingService.getUserBookings(currentUser, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking by id", description = "Return a booking that belongs to the current authenticated user.")
    @ApiResponse(responseCode = "200", description = "Booking returned successfully.",
            content = @Content(schema = @Schema(implementation = BookingResponse.class)))
    @CommonApiResponses
    public ResponseEntity<BookingResponse> getBooking(@PathVariable UUID id,
                                               @Parameter(hidden = true)
                                               @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(bookingService.getBooking(id, currentUser));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel booking", description = "Cancel a booking that belongs to the current authenticated user.")
    @ApiResponse(responseCode = "200", description = "Booking cancelled successfully.",
            content = @Content(schema = @Schema(implementation = ApiMessageResponse.class)))
    @CommonApiResponses
    public ResponseEntity<ApiMessageResponse> cancelBooking(
            @PathVariable UUID id,
            @Valid @RequestBody CancelBookingRequest request,
            @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(bookingService.cancelBooking(id, currentUser, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update booking status", description = "Move a booking through the allowed status state machine. Admin only.")
    @ApiResponse(responseCode = "200", description = "Booking status updated successfully.",
            content = @Content(schema = @Schema(implementation = BookingResponse.class)))
    @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden")
    @CommonApiResponses
    public ResponseEntity<BookingResponse> updateBookingStatus(
            @PathVariable UUID id,
            @Valid @RequestBody BookingStatusUpdateRequest request,
            @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(bookingService.updateBookingStatus(id, request, currentUser));
    }

    @PatchMapping("/{id}/check-in")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    @Operation(
            summary = "Check-in booking",
            description = "Check-in a confirmed booking. Admin or receptionist only. "
                    + "Local/dev receptionist account: reception@booking.local / admin123."
    )
    @ApiResponse(responseCode = "200", description = "Booking checked-in successfully.",
                content = @Content(schema = @Schema(implementation = ApiMessageResponse.class)))
    @CommonApiResponses
    public ResponseEntity<ApiMessageResponse> checkInBooking(
            @PathVariable UUID id,
            @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(bookingService.checkInBooking(id, currentUser));
    }

    @PatchMapping("/{id}/check-out")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    @Operation(
            summary = "Check-out booking",
            description = "Check-out a checked-in booking. Admin or receptionist only. "
                    + "Local/dev receptionist account: reception@booking.local / admin123."
    )
    @ApiResponse(responseCode = "200", description = "Booking checked-out successfully.",
            content = @Content(schema = @Schema(implementation = ApiMessageResponse.class)))
    @CommonApiResponses
    public ResponseEntity<ApiMessageResponse> checkOutBooking(
            @PathVariable UUID id,
            @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(bookingService.checkOutBooking(id, currentUser));
    }

    @PatchMapping("/{id}/no-show")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    @Operation(
            summary = "Mark booking as no-show",
            description = "Mark a confirmed booking as no-show. Admin or receptionist only. "
                    + "Local/dev receptionist account: reception@booking.local / admin123."
    )
    @ApiResponse(responseCode = "200", description = "Booking marked as no-show successfully.",
            content = @Content(schema = @Schema(implementation = ApiMessageResponse.class)))
    @CommonApiResponses
    public ResponseEntity<ApiMessageResponse> markNoShow(
            @PathVariable UUID id,
            @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(bookingService.markNoShow(id, currentUser));
    }

}

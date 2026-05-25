package com.example.bookingapi.controller;

import com.example.bookingapi.model.Booking;
import com.example.bookingapi.payload.request.BookingRequest;
import com.example.bookingapi.payload.response.PagedResponse;
import com.example.bookingapi.security.CurrentUser;
import com.example.bookingapi.security.UserPrincipal;
import com.example.bookingapi.service.BookingService;
import com.example.bookingapi.utils.AppConstants;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Bookings", description = "Booking management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @PostMapping
    @Operation(summary = "Create booking", description = "Create a new booking for the current authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Booking created successfully.",
                    content = @Content(schema = @Schema(implementation = Booking.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    public ResponseEntity<Booking> createBooking(@Valid @RequestBody BookingRequest bookingRequest,
                                                  @Parameter(hidden = true)
                                                  @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBooking(bookingRequest, currentUser));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my bookings", description = "Return paginated bookings of the current authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user's bookings returned successfully.",
                    content = @Content(schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    public ResponseEntity<PagedResponse<Booking>> getMyBookings(
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking returned successfully.",
                    content = @Content(schema = @Schema(implementation = Booking.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    public ResponseEntity<Booking> getBooking(@PathVariable UUID id,
                                               @Parameter(hidden = true)
                                               @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(bookingService.getBooking(id, currentUser));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel booking", description = "Cancel a booking that belongs to the current authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking cancelled successfully.",
                    content = @Content(schema = @Schema(implementation = com.example.bookingapi.payload.response.ApiResponse.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    public ResponseEntity<com.example.bookingapi.payload.response.ApiResponse> cancelBooking(@PathVariable UUID id,
                                                      @Parameter(hidden = true)
                                                      @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(bookingService.cancelBooking(id, currentUser));
    }
}

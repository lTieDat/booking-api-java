package com.example.bookingapi.features.receptionist.controller;

import com.example.bookingapi.common.openapi.CommonApiResponses;
import com.example.bookingapi.common.response.ApiMessageResponse;
import com.example.bookingapi.common.response.PagedResponse;
import com.example.bookingapi.common.security.CurrentUser;
import com.example.bookingapi.common.security.UserPrincipal;
import com.example.bookingapi.common.util.AppConstants;
import com.example.bookingapi.features.booking.dto.response.BookingResponse;
import com.example.bookingapi.features.booking.model.enums.BookingStatus;
import com.example.bookingapi.features.receptionist.dto.request.ReceptionistAssignmentRequest;
import com.example.bookingapi.features.receptionist.dto.response.ReceptionistAssignmentResponse;
import com.example.bookingapi.features.receptionist.dto.response.ReceptionistHotelResponse;
import com.example.bookingapi.features.receptionist.service.ReceptionistService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/receptionist")
@Tag(
        name = "Receptionist",
        description = "Receptionist hotel assignment and front-desk booking workflow. "
                + "Local/dev receptionist: reception@booking.local / admin123."
)
@SecurityRequirement(name = "bearerAuth")
public class ReceptionistController {

    private final ReceptionistService receptionistService;

    public ReceptionistController(ReceptionistService receptionistService) {
        this.receptionistService = receptionistService;
    }

    @PostMapping("/assignments")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Assign receptionist to hotel",
            description = "Admin assigns a user to a hotel and grants `ROLE_RECEPTIONIST` if the user does not already have it."
    )
    @ApiResponse(responseCode = "201", description = "Receptionist assignment created or reactivated.",
            content = @Content(schema = @Schema(implementation = ReceptionistAssignmentResponse.class)))
    @CommonApiResponses
    public ResponseEntity<ReceptionistAssignmentResponse> assignReceptionist(
            @Valid @RequestBody ReceptionistAssignmentRequest request,
            @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(receptionistService.assignReceptionist(request, currentUser));
    }

    @GetMapping("/assignments")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List receptionist assignments", description = "Admin lists receptionist hotel assignments.")
    @ApiResponse(responseCode = "200", description = "Receptionist assignments returned successfully.",
            content = @Content(schema = @Schema(implementation = PagedResponse.class)))
    @CommonApiResponses
    public ResponseEntity<PagedResponse<ReceptionistAssignmentResponse>> getAssignments(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID hotelId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        return ResponseEntity.ok(receptionistService.getAssignments(userId, hotelId, active, page, size));
    }

    @DeleteMapping("/assignments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate receptionist assignment", description = "Admin deactivates a receptionist hotel assignment.")
    @ApiResponse(responseCode = "200", description = "Receptionist assignment deactivated successfully.",
            content = @Content(schema = @Schema(implementation = ApiMessageResponse.class)))
    @CommonApiResponses
    public ResponseEntity<ApiMessageResponse> deactivateAssignment(@PathVariable UUID id) {
        return ResponseEntity.ok(receptionistService.deactivateAssignment(id));
    }

    @GetMapping("/me/hotels")
    @PreAuthorize("hasRole('RECEPTIONIST')")
    @Operation(summary = "Get my assigned hotels", description = "Receptionist returns active hotel assignments for the current account.")
    @ApiResponse(responseCode = "200", description = "Assigned hotels returned successfully.",
            content = @Content(schema = @Schema(implementation = ReceptionistHotelResponse.class)))
    @CommonApiResponses
    public ResponseEntity<List<ReceptionistHotelResponse>> getMyHotels(
            @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(receptionistService.getMyHotels(currentUser));
    }

    @GetMapping("/hotels/{hotelId}/bookings")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    @Operation(
            summary = "List hotel bookings for front desk",
            description = "Admin can list any hotel's bookings. Receptionist can list only assigned hotels. "
                    + "Use `status=CONFIRMED` for check-in/no-show queue and `status=CHECKED_IN` for check-out queue."
    )
    @ApiResponse(responseCode = "200", description = "Hotel bookings returned successfully.",
            content = @Content(schema = @Schema(implementation = PagedResponse.class)))
    @CommonApiResponses
    public ResponseEntity<PagedResponse<BookingResponse>> getHotelBookings(
            @PathVariable UUID hotelId,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(receptionistService.getHotelBookings(hotelId, status, page, size, currentUser));
    }
}

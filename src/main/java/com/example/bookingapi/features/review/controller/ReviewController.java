package com.example.bookingapi.features.review.controller;

import com.example.bookingapi.common.openapi.CommonApiResponses;
import com.example.bookingapi.common.response.ApiMessageResponse;
import com.example.bookingapi.common.response.PagedResponse;
import com.example.bookingapi.common.security.CurrentUser;
import com.example.bookingapi.common.security.UserPrincipal;
import com.example.bookingapi.common.util.AppConstants;
import com.example.bookingapi.features.review.dto.request.ReviewRequest;
import com.example.bookingapi.features.review.dto.response.ReviewResponse;
import com.example.bookingapi.features.review.service.ReviewService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = "Reviews", description = "Hotel review endpoints for checked-out bookings.")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/api/bookings/{bookingId}/review")
    @PreAuthorize("hasRole('USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Review booking", description = "Create one review for a checked-out booking owned by the current user.")
    @ApiResponse(responseCode = "201", description = "Review created successfully.",
            content = @Content(schema = @Schema(implementation = ReviewResponse.class)))
    @CommonApiResponses
    public ResponseEntity<ReviewResponse> createBookingReview(
            @PathVariable UUID bookingId,
            @Valid @RequestBody ReviewRequest request,
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createBookingReview(bookingId, request, currentUser));
    }

    @PutMapping("/api/reviews/{reviewId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update review", description = "Owner or admin updates a review.")
    @CommonApiResponses
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable UUID reviewId,
            @Valid @RequestBody ReviewRequest request,
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(reviewService.updateReview(reviewId, request, currentUser));
    }

    @DeleteMapping("/api/reviews/{reviewId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Hide review", description = "Owner or admin hides a review from public hotel reviews.")
    @CommonApiResponses
    public ResponseEntity<ApiMessageResponse> hideReview(
            @PathVariable UUID reviewId,
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(reviewService.hideReview(reviewId, currentUser));
    }

    @GetMapping("/api/hotels/{hotelId}/reviews")
    @Operation(summary = "List hotel reviews", description = "Return public visible reviews for a hotel.")
    @CommonApiResponses
    public ResponseEntity<PagedResponse<ReviewResponse>> getHotelReviews(
            @PathVariable UUID hotelId,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        return ResponseEntity.ok(reviewService.getHotelReviews(hotelId, page, size));
    }

    @GetMapping("/api/reviews/me")
    @PreAuthorize("hasRole('USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List my reviews", description = "Return reviews written by the current user.")
    @CommonApiResponses
    public ResponseEntity<PagedResponse<ReviewResponse>> getMyReviews(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        return ResponseEntity.ok(reviewService.getMyReviews(currentUser, page, size));
    }
}

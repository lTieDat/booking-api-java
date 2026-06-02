package com.example.bookingapi.features.review.service;

import com.example.bookingapi.common.response.ApiMessageResponse;
import com.example.bookingapi.common.response.PagedResponse;
import com.example.bookingapi.common.security.UserPrincipal;
import com.example.bookingapi.features.review.dto.request.ReviewRequest;
import com.example.bookingapi.features.review.dto.response.ReviewResponse;

import java.util.UUID;

public interface ReviewService {
    ReviewResponse createBookingReview(UUID bookingId, ReviewRequest request, UserPrincipal currentUser);
    ReviewResponse updateReview(UUID reviewId, ReviewRequest request, UserPrincipal currentUser);
    ApiMessageResponse hideReview(UUID reviewId, UserPrincipal currentUser);
    PagedResponse<ReviewResponse> getHotelReviews(UUID hotelId, int page, int size);
    PagedResponse<ReviewResponse> getMyReviews(UserPrincipal currentUser, int page, int size);
}

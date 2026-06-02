package com.example.bookingapi.features.review.service.impl;

import com.example.bookingapi.common.exception.BadRequestException;
import com.example.bookingapi.common.exception.ResourceNotFoundException;
import com.example.bookingapi.common.response.ApiMessageResponse;
import com.example.bookingapi.common.response.PagedResponse;
import com.example.bookingapi.common.security.UserPrincipal;
import com.example.bookingapi.features.booking.model.Booking;
import com.example.bookingapi.features.booking.model.enums.BookingStatus;
import com.example.bookingapi.features.booking.repository.BookingRepository;
import com.example.bookingapi.features.hotel.model.Hotel;
import com.example.bookingapi.features.review.dto.request.ReviewRequest;
import com.example.bookingapi.features.review.dto.response.ReviewResponse;
import com.example.bookingapi.features.review.model.Review;
import com.example.bookingapi.features.review.repository.ReviewRepository;
import com.example.bookingapi.features.review.service.ReviewService;
import com.example.bookingapi.features.user.model.User;
import com.example.bookingapi.features.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    public ReviewServiceImpl(
            ReviewRepository reviewRepository,
            BookingRepository bookingRepository,
            UserRepository userRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ReviewResponse createBookingReview(UUID bookingId, ReviewRequest request, UserPrincipal currentUser) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You don't have permission to review this booking");
        }
        if (booking.getStatus() != BookingStatus.CHECKED_OUT) {
            throw new BadRequestException("Only checked-out bookings can be reviewed");
        }
        if (reviewRepository.existsByBooking_Id(bookingId)) {
            throw new BadRequestException("Booking already has a review");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        Review review = new Review();
        review.setBooking(booking);
        review.setUser(user);
        review.setHotel(resolveSingleHotel(booking));
        mapReview(review, request);
        review.setIsVisible(true);
        return toResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(UUID reviewId, ReviewRequest request, UserPrincipal currentUser) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
        assertOwnerOrAdmin(review, currentUser);
        mapReview(review, request);
        return toResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional
    public ApiMessageResponse hideReview(UUID reviewId, UserPrincipal currentUser) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
        assertOwnerOrAdmin(review, currentUser);
        review.setIsVisible(false);
        reviewRepository.save(review);
        return new ApiMessageResponse(true, "Review hidden successfully");
    }

    @Override
    public PagedResponse<ReviewResponse> getHotelReviews(UUID hotelId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Review> reviews = reviewRepository.findByHotel_IdAndIsVisibleTrue(hotelId, pageable);
        return toPagedResponse(reviews);
    }

    @Override
    public PagedResponse<ReviewResponse> getMyReviews(UserPrincipal currentUser, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Review> reviews = reviewRepository.findByUser_Id(currentUser.getId(), pageable);
        return toPagedResponse(reviews);
    }

    private void mapReview(Review review, ReviewRequest request) {
        review.setRating(request.getRating());
        review.setTitle(request.getTitle() == null ? null : request.getTitle().trim());
        review.setComment(request.getComment() == null ? null : request.getComment().trim());
    }

    private Hotel resolveSingleHotel(Booking booking) {
        Set<Hotel> hotels = booking.getBookedRooms().stream()
                .map(bookedRoom -> bookedRoom.getRoomType().getHotel())
                .collect(Collectors.toSet());
        if (hotels.size() != 1) {
            throw new BadRequestException("Review is supported only for single-hotel bookings");
        }
        return hotels.iterator().next();
    }

    private void assertOwnerOrAdmin(Review review, UserPrincipal currentUser) {
        if (hasRole(currentUser, "ROLE_ADMIN")) {
            return;
        }
        if (!review.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You don't have permission to update this review");
        }
    }

    private boolean hasRole(UserPrincipal currentUser, String roleName) {
        return currentUser.getAuthorities().stream()
                .anyMatch(authority -> roleName.equals(authority.getAuthority()));
    }

    private PagedResponse<ReviewResponse> toPagedResponse(Page<Review> reviews) {
        List<ReviewResponse> content = reviews.getContent().stream()
                .map(this::toResponse)
                .toList();
        return new PagedResponse<>(content, reviews.getNumber(), reviews.getSize(),
                reviews.getTotalElements(), reviews.getTotalPages(), reviews.isLast());
    }

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getBooking().getId(),
                review.getUser().getId(),
                review.getUser().getName(),
                review.getHotel().getId(),
                review.getHotel().getName(),
                review.getRating(),
                review.getTitle(),
                review.getComment(),
                review.getIsVisible(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}

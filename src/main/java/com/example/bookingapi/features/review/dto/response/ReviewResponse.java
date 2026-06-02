package com.example.bookingapi.features.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class ReviewResponse {
    private UUID id;
    private UUID bookingId;
    private UUID userId;
    private String userName;
    private UUID hotelId;
    private String hotelName;
    private Integer rating;
    private String title;
    private String comment;
    private Boolean visible;
    private Instant createdAt;
    private Instant updatedAt;
}

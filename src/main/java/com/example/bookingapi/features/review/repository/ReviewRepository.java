package com.example.bookingapi.features.review.repository;

import com.example.bookingapi.features.review.model.Review;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    boolean existsByBooking_Id(UUID bookingId);
    Optional<Review> findByBooking_Id(UUID bookingId);
    Page<Review> findByHotel_IdAndIsVisibleTrue(UUID hotelId, Pageable pageable);
    Page<Review> findByUser_Id(UUID userId, Pageable pageable);
    long countByHotel_IdAndIsVisibleTrue(UUID hotelId);

    @Query("""
            SELECT COALESCE(AVG(r.rating), 0)
            FROM Review r
            WHERE r.hotel.id = :hotelId
              AND r.isVisible = true
            """)
    Double getAverageVisibleRatingByHotelId(@Param("hotelId") UUID hotelId);
}

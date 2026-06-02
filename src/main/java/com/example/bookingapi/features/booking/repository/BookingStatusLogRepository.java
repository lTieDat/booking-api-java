package com.example.bookingapi.features.booking.repository;

import com.example.bookingapi.features.booking.model.BookingStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingStatusLogRepository extends JpaRepository<BookingStatusLog, UUID> {
    List<BookingStatusLog> findByBookingIdOrderByCreatedAtAsc(UUID bookingId);
}

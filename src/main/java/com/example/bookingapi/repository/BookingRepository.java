package com.example.bookingapi.repository;

import com.example.bookingapi.model.Booking;
import com.example.bookingapi.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    Page<Booking> findByUser(User user, Pageable pageable);
}

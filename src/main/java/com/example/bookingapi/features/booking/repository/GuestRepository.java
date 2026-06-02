package com.example.bookingapi.features.booking.repository;

import com.example.bookingapi.features.booking.model.Guest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GuestRepository extends JpaRepository<Guest, UUID> {
    Optional<Guest> findByEmail(String email);
    Optional<Guest> findByIdentifyCardNo(String identifyCardNo);
}

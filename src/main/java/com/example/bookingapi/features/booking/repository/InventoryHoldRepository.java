package com.example.bookingapi.features.booking.repository;

import com.example.bookingapi.features.booking.model.InventoryHold;
import com.example.bookingapi.features.booking.model.enums.InventoryHoldStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryHoldRepository extends JpaRepository<InventoryHold, UUID> {
    List<InventoryHold> findByBooking_IdAndStatus(UUID bookingId, InventoryHoldStatus status);
}

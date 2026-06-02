package com.example.bookingapi.features.booking.repository;

import com.example.bookingapi.features.booking.model.RoomInventory;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomInventoryRepository extends JpaRepository<RoomInventory, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    Optional<RoomInventory> findByRoomType_IdAndDate(UUID roomTypeId, LocalDate date);
}

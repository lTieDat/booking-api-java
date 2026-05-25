package com.example.bookingapi.repository;

import com.example.bookingapi.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {
    List<Room> findByHotelId(UUID hotelId);
    Optional<Room> findByIdAndHotelId(UUID id, UUID hotelId);
}

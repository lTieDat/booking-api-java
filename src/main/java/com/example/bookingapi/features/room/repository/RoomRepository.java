package com.example.bookingapi.features.room.repository;

import com.example.bookingapi.features.room.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {
    List<Room> findByRoomType_Hotel_Id(UUID hotelId);

    Optional<Room> findByIdAndRoomType_Hotel_Id(UUID id, UUID hotelId);

    long countByRoomType_IdAndIsActiveTrue(UUID roomTypeId);
}

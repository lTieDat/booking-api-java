package com.example.bookingapi.features.room.repository;

import com.example.bookingapi.features.room.model.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, UUID> {
    List<RoomType> findByHotelId(UUID hotelId);

    Optional<RoomType> findByIdAndHotelId(UUID id, UUID hotelId);
}

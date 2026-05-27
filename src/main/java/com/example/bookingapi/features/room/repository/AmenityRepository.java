package com.example.bookingapi.features.room.repository;

import com.example.bookingapi.features.room.model.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, UUID> {
    List<Amenity> findByRoomTypeId(UUID roomTypeId);

    Optional<Amenity> findByIdAndRoomTypeId(UUID id, UUID roomTypeId);

    boolean existsByRoomTypeIdAndCode(UUID roomTypeId, String code);
}

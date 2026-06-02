package com.example.bookingapi.features.receptionist.repository;

import com.example.bookingapi.features.receptionist.model.ReceptionistAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReceptionistAssignmentRepository extends JpaRepository<ReceptionistAssignment, UUID> {

    boolean existsByUser_IdAndHotel_IdAndIsActiveTrue(UUID userId, UUID hotelId);

    Optional<ReceptionistAssignment> findByUser_IdAndHotel_Id(UUID userId, UUID hotelId);

    @EntityGraph(attributePaths = {"user", "hotel", "hotel.location", "hotel.images"})
    List<ReceptionistAssignment> findByUser_IdAndIsActiveTrue(UUID userId);

    @EntityGraph(attributePaths = {"user", "hotel", "hotel.location"})
    Page<ReceptionistAssignment> findByUser_Id(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "hotel", "hotel.location"})
    Page<ReceptionistAssignment> findByHotel_Id(UUID hotelId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "hotel", "hotel.location"})
    Page<ReceptionistAssignment> findByIsActive(Boolean isActive, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "hotel", "hotel.location"})
    Page<ReceptionistAssignment> findByUser_IdAndHotel_Id(UUID userId, UUID hotelId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "hotel", "hotel.location"})
    Page<ReceptionistAssignment> findByUser_IdAndIsActive(UUID userId, Boolean isActive, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "hotel", "hotel.location"})
    Page<ReceptionistAssignment> findByHotel_IdAndIsActive(UUID hotelId, Boolean isActive, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "hotel", "hotel.location"})
    Page<ReceptionistAssignment> findByUser_IdAndHotel_IdAndIsActive(
            UUID userId,
            UUID hotelId,
            Boolean isActive,
            Pageable pageable
    );
}

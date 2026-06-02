package com.example.bookingapi.features.booking.repository;

import com.example.bookingapi.features.booking.model.CancellationPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CancellationPolicyRepository extends JpaRepository<CancellationPolicy, UUID> {
    Page<CancellationPolicy> findByHotel_Id(UUID hotelId, Pageable pageable);
    List<CancellationPolicy> findByHotel_IdAndIsActiveTrue(UUID hotelId);
}

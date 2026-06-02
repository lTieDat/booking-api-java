package com.example.bookingapi.features.booking.repository;

import com.example.bookingapi.features.booking.model.Discount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, UUID> {
    Optional<Discount> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    Page<Discount> findByIsActive(Boolean isActive, Pageable pageable);
}

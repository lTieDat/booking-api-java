package com.example.bookingapi.features.invoice.repository;

import com.example.bookingapi.features.invoice.model.TaxConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaxConfigRepository extends JpaRepository<TaxConfig, UUID> {
    List<TaxConfig> findByActiveTrue();
    List<TaxConfig> findByHotel_IdAndActiveTrue(UUID hotelId);
}

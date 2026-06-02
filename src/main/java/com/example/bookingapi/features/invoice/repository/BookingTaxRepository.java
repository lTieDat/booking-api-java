package com.example.bookingapi.features.invoice.repository;

import com.example.bookingapi.features.invoice.model.BookingTax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingTaxRepository extends JpaRepository<BookingTax, UUID> {
    List<BookingTax> findByBooking_Id(UUID bookingId);
}

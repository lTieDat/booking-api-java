package com.example.bookingapi.features.invoice.repository;

import com.example.bookingapi.features.invoice.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Optional<Invoice> findByPayment_Id(UUID paymentId);
    Optional<Invoice> findByBooking_Id(UUID bookingId);
    Optional<Invoice> findByInvoiceNo(String invoiceNo);
    boolean existsByInvoiceNo(String invoiceNo);
}

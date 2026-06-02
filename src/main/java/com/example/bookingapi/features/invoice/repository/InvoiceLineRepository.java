package com.example.bookingapi.features.invoice.repository;

import com.example.bookingapi.features.invoice.model.InvoiceLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, UUID> {
    List<InvoiceLine> findByInvoice_Id(UUID invoiceId);
}

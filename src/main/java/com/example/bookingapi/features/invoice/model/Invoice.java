package com.example.bookingapi.features.invoice.model;

import com.example.bookingapi.common.audit.DateAudit;
import com.example.bookingapi.features.booking.model.Booking;
import com.example.bookingapi.features.invoice.model.enums.InvoiceStatus;
import com.example.bookingapi.features.payment.model.Payment;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "invoices",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_invoices_invoice_no", columnNames = "invoice_no"),
                @UniqueConstraint(name = "uk_invoices_booking_payment", columnNames = {"booking_id", "payment_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Invoice extends DateAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "invoice_no", nullable = false, length = 50)
    private String invoiceNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "subtotal_minor", nullable = false)
    private Long subtotalMinor;

    @Column(name = "discount_minor", nullable = false)
    private Long discountMinor = 0L;

    @Column(name = "tax_minor", nullable = false)
    private Long taxMinor = 0L;

    @Column(name = "total_minor", nullable = false)
    private Long totalMinor;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "VND";

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "voided_at")
    private LocalDateTime voidedAt;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceLine> lines = new ArrayList<>();

    public void addLine(InvoiceLine line) {
        lines.add(line);
        line.setInvoice(this);
    }
}

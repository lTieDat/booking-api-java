package com.example.bookingapi.features.invoice.model;

import com.example.bookingapi.features.booking.model.Booking;
import com.example.bookingapi.features.invoice.model.enums.TaxApplyType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "booking_taxes")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class BookingTax {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_config_id")
    private TaxConfig taxConfig;

    @Column(name = "tax_name", nullable = false, length = 100)
    private String taxName;

    @Enumerated(EnumType.STRING)
    @Column(name = "apply_type", nullable = false, length = 40)
    private TaxApplyType applyType;

    @Column(name = "rate", precision = 8, scale = 4)
    private BigDecimal rate;

    @Column(name = "amount_minor", nullable = false)
    private Long amountMinor;

    @Column(name = "is_inclusive", nullable = false)
    private Boolean inclusive;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

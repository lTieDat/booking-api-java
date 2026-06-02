package com.example.bookingapi.features.invoice.model;

import com.example.bookingapi.common.audit.DateAudit;
import com.example.bookingapi.features.hotel.model.Hotel;
import com.example.bookingapi.features.invoice.model.enums.TaxApplyType;
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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "tax_configs")
@Getter
@Setter
@NoArgsConstructor
public class TaxConfig extends DateAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "apply_type", nullable = false, length = 40)
    private TaxApplyType applyType;

    @Column(name = "rate", precision = 8, scale = 4)
    private BigDecimal rate;

    @Column(name = "amount_minor")
    private Long amountMinor;

    @Column(name = "is_inclusive", nullable = false)
    private Boolean inclusive = false;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;
}

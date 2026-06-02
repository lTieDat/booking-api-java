package com.example.bookingapi.features.booking.model;

import com.example.bookingapi.common.audit.UserDateAudit;
import com.example.bookingapi.features.hotel.model.Hotel;
import com.example.bookingapi.features.booking.model.enums.CancellationPenaltyType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
        name = "cancellation_policies"
)
@Getter
@Setter
@NoArgsConstructor
public class CancellationPolicy extends UserDateAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "free_cancellation_hours", nullable = false)
    private Integer freeCancellationHours = 24;

    @Enumerated(EnumType.STRING)
    @Column(name = "penalty_type", nullable = false, length = 30)
    private CancellationPenaltyType penaltyType = CancellationPenaltyType.NONE;

    @Column(name = "penalty_value", nullable = false)
    private Long penaltyValue = 0L;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;
}

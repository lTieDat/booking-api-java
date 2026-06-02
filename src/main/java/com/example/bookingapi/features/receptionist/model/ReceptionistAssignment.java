package com.example.bookingapi.features.receptionist.model;

import com.example.bookingapi.common.audit.UserDateAudit;
import com.example.bookingapi.features.hotel.model.Hotel;
import com.example.bookingapi.features.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
        name = "receptionist_assignments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_receptionist_assignments_user_hotel", columnNames = {"user_id", "hotel_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ReceptionistAssignment extends UserDateAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}

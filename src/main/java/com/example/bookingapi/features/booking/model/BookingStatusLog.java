package com.example.bookingapi.features.booking.model;

import com.example.bookingapi.common.audit.DateAudit;
import com.example.bookingapi.features.auth.model.enums.ActorType;
import com.example.bookingapi.features.booking.model.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "booking_status_logs",
        indexes = {
                @Index(name = "idx_booking_status_logs_booking_id", columnList = "booking_id"),
                @Index(name = "idx_booking_status_logs_performed_by", columnList = "performed_by"),
                @Index(name = "idx_booking_status_logs_status_created_at", columnList = "to_status, created_at")
        })
@Getter
@Setter
@NoArgsConstructor
public class BookingStatusLog extends DateAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private BookingStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private BookingStatus toStatus;

    @Column(name = "performed_by")
    private UUID performedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "performed_by_type", nullable = false, length = 30)
    private ActorType performedByType = ActorType.SYSTEM;

    @Column(name = "note", length = 500)
    private String note;
}

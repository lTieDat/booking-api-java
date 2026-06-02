package com.example.bookingapi.features.booking.model;

import com.example.bookingapi.common.audit.DateAudit;
import com.example.bookingapi.features.room.model.RoomType;
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

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "room_inventories",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_room_inventories_room_type_date", columnNames = {"room_type_id", "date"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class RoomInventory extends DateAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "total_units", nullable = false)
    private Integer totalUnits = 0;

    @Column(name = "booked_units", nullable = false)
    private Integer bookedUnits = 0;

    @Column(name = "held_units", nullable = false)
    private Integer heldUnits = 0;

    @Column(name = "available_units", nullable = false)
    private Integer availableUnits = 0;

    public void hold(int quantity) {
        heldUnits += quantity;
        availableUnits -= quantity;
    }

    public void releaseHold(int quantity) {
        heldUnits -= quantity;
        availableUnits += quantity;
    }

    public void consumeHold(int quantity) {
        heldUnits -= quantity;
        bookedUnits += quantity;
    }

    public void releaseBooked(int quantity) {
        bookedUnits -= quantity;
        availableUnits += quantity;
    }
}

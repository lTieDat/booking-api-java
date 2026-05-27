package com.example.bookingapi.features.room.model;

import com.example.bookingapi.common.audit.UserDateAudit;
import com.example.bookingapi.features.room.model.enums.RoomStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "rooms",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_rooms_room_type_number", columnNames = {"room_type_id", "room_number"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Room extends UserDateAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 20)
    @Column(name = "room_number", nullable = false)
    private String roomNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @Column(name = "floor")
    private Integer floor;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private RoomStatus status;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}

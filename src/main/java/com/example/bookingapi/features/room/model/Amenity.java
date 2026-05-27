package com.example.bookingapi.features.room.model;

import com.example.bookingapi.common.audit.UserDateAudit;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
        name = "amenities",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_amenities_room_type_code", columnNames = {"room_type_id", "code"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Amenity extends UserDateAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @NotBlank
    @Size(max = 50)
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @NotBlank
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 300)
    @Column(name = "description", length = 300)
    private String description;

    @Min(1)
    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}

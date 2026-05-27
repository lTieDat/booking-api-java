package com.example.bookingapi.features.room.model;

import com.example.bookingapi.common.audit.UserDateAudit;
import com.example.bookingapi.features.hotel.model.Hotel;
import com.example.bookingapi.features.room.model.enums.BedType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "room_types",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_room_types_hotel_code", columnNames = {"hotel_id", "code"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class RoomType extends UserDateAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @NotBlank
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @NotBlank
    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "max_adults", nullable = false)
    @Min(1)
    private Integer maxAdults;

    @Column(name = "max_children", nullable = false)
    @Min(0)
    private Integer maxChildren;

    @Column(name = "max_occupancy", nullable = false)
    @Min(1)
    private Integer maxOccupancy;

    @Column(name = "bed_type")
    @Enumerated(EnumType.STRING)
    private BedType bedType;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "base_price", precision = 10, scale = 2)
    @Positive
    private BigDecimal basePrice;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @JsonIgnore
    @OneToMany(mappedBy = "roomType", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Room> rooms = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "roomType", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Amenity> amenities = new ArrayList<>();
}

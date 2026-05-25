package com.example.bookingapi.model;

import com.example.bookingapi.model.audit.UserDateAudit;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
public class Room extends UserDateAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @NotBlank
    @Size(max = 20)
    @Column(name = "room_number", nullable = false)
    private String roomNumber;

    @Size(max = 50)
    @Column(name = "room_type")
    private String roomType;

    private Integer capacity;

    @Column(name = "price_per_night", precision = 10, scale = 2)
    private BigDecimal pricePerNight;
}

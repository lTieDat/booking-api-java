package com.example.bookingapi.features.booking.model;

import com.example.bookingapi.features.room.model.RoomType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "booked_rooms")
@Getter
@Setter
@NoArgsConstructor
public class BookedRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @NotNull
    @Min(1)
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @NotNull
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "room_type_name_snapshot", length = 50)
    private String roomTypeNameSnapshot;

    @Column(name = "room_type_code_snapshot", length = 30)
    private String roomTypeCodeSnapshot;

    @Column(name = "bed_type_snapshot", length = 50)
    private String bedTypeSnapshot;

    @Column(name = "max_occupancy_snapshot")
    private Integer maxOccupancySnapshot;
}

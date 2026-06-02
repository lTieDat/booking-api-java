package com.example.bookingapi.features.booking.model;

import com.example.bookingapi.common.audit.UserDateAudit;
import com.example.bookingapi.features.booking.model.enums.BookingStatus;
import com.example.bookingapi.features.user.model.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
public class Booking extends UserDateAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookedRoom> bookedRooms = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id")
    private Guest guest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_id")
    private Discount discount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancellation_policy_id")
    private CancellationPolicy cancellationPolicy;

    @Column(name = "check_in_date_time", nullable = false)
    private LocalDateTime checkInDateTime;

    @Column(name = "check_out_date_time", nullable = false)
    @FutureOrPresent
    private LocalDateTime checkOutDateTime;

    @Column(name = "actual_check_out_date")
    @FutureOrPresent
    private LocalDateTime actualCheckOutDate;

    @Column(name="actual_check_in_date")
    private LocalDateTime actualCheckInDate;

    @Column(name = "total_guest" )
    @Min(1)
    private Integer totalGuest;

    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "VND";

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name="expired_at")
    @FutureOrPresent
    private LocalDateTime expiredAt;

    @Column(name = "note", length = 300)
    private String note;

    @Column(name = "client_request_id", length = 120)
    private String clientRequestId;

    @Column(name = "request_hash", length = 128)
    private String requestHash;

    public void addBookedRoom(BookedRoom bookedRoom) {
        bookedRooms.add(bookedRoom);
        bookedRoom.setBooking(this);
    }
}

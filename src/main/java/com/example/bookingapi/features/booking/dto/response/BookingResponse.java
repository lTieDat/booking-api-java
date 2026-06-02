package com.example.bookingapi.features.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class BookingResponse {
    private UUID id;
    private List<BookedRoomResponse> bookedRooms;
    private LocalDateTime checkInDateTime;
    private LocalDateTime checkOutDateTime;
    private BigDecimal totalPrice;
    private BigDecimal discountAmount;
    private BigDecimal cancellationFee;
    private String currency;
    private String status;
    private GuestResponse guest;
}

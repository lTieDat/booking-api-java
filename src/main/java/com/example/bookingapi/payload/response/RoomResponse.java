package com.example.bookingapi.payload.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class RoomResponse {
    private UUID id;
    private UUID hotelId;
    private String roomNumber;
    private String roomType;
    private Integer capacity;
    private BigDecimal pricePerNight;
}


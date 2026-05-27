package com.example.bookingapi.features.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class BookedRoomResponse {
    private UUID id;
    private UUID roomTypeId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private String roomTypeNameSnapshot;
    private String roomTypeCodeSnapshot;
    private String bedTypeSnapshot;
    private Integer maxOccupancySnapshot;
}

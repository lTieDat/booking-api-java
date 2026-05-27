package com.example.bookingapi.features.room.dto.response;

import com.example.bookingapi.features.room.model.enums.BedType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class RoomTypeResponse {
    private UUID id;
    private UUID hotelId;
    private String name;
    private String code;
    private Integer maxAdults;
    private Integer maxChildren;
    private Integer maxOccupancy;
    private BedType bedType;
    private String description;
    private BigDecimal basePrice;
    private List<AmenityResponse> amenities;
    private Boolean isActive;
}

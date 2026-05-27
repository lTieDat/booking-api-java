package com.example.bookingapi.features.room.dto.response;

import com.example.bookingapi.features.room.model.enums.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class RoomResponse {
    private UUID id;
    private UUID hotelId;
    private UUID roomTypeId;
    private String roomTypeName;
    private String roomTypeCode;
    private String roomNumber;
    private Integer floor;
    private RoomStatus status;
    private Boolean isActive;
}

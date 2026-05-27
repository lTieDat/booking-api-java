package com.example.bookingapi.features.room.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import com.example.bookingapi.features.room.model.enums.RoomStatus;
import java.util.UUID;

@Getter
@Setter
public class RoomRequest {

    @NotNull
    private UUID roomTypeId;

    @NotBlank
    @Size(max = 20)
    private String roomNumber;

    private Integer floor;

    private RoomStatus status;

    private Boolean isActive;
}

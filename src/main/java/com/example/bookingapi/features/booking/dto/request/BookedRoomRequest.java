package com.example.bookingapi.features.booking.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class BookedRoomRequest {

    @NotNull
    private UUID roomTypeId;

    @NotNull
    @Min(1)
    private Integer quantity;
}

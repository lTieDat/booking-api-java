package com.example.bookingapi.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RoomRequest {

    @NotBlank
    @Size(max = 20)
    private String roomNumber;

    @Size(max = 50)
    private String roomType;

    private Integer capacity;

    @NotNull
    private BigDecimal pricePerNight;
}

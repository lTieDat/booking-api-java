package com.example.bookingapi.payload.request;

import jakarta.validation.constraints.*;
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

    @Positive
    @Min(1)
    private Integer capacity;

    @NotNull
    @Positive
    private BigDecimal pricePerNight;
}

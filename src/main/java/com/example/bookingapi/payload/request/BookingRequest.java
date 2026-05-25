package com.example.bookingapi.payload.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class BookingRequest {

    @NotNull
    private UUID roomId;

    @NotNull
    private LocalDate checkInDate;

    @NotNull
    @Future
    private LocalDate checkOutDate;
}

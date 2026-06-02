package com.example.bookingapi.features.booking.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class BookingRequest {

    @NotEmpty
    @Valid
    private List<BookedRoomRequest> rooms;

    @NotNull
    @FutureOrPresent
    private LocalDateTime checkInDate;

    @NotNull
    @Future
    private LocalDateTime checkOutDate;

    @NotNull
    @Valid
    private BookingGuestRequest guest;
}

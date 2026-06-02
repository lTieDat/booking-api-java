package com.example.bookingapi.features.booking.dto.request;

import com.example.bookingapi.features.booking.model.enums.BookingStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingStatusUpdateRequest {

    @NotNull
    private BookingStatus status;

    @Size(max = 500)
    private String reason;
}

package com.example.bookingapi.features.receptionist.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ReceptionistAssignmentRequest {

    @NotNull
    private UUID userId;

    @NotNull
    private UUID hotelId;

    private Boolean active = true;
}

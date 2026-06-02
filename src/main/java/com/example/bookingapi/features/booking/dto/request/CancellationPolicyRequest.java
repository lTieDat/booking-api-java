package com.example.bookingapi.features.booking.dto.request;

import com.example.bookingapi.features.booking.model.enums.CancellationPenaltyType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CancellationPolicyRequest {

    private UUID hotelId;

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    @NotNull
    @Min(0)
    private Integer freeCancellationHours = 24;

    @NotNull
    private CancellationPenaltyType penaltyType = CancellationPenaltyType.NONE;

    @NotNull
    @Min(0)
    private Long penaltyValue = 0L;

    private Boolean active = false;
}

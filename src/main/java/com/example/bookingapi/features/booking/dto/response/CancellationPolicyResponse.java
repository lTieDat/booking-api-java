package com.example.bookingapi.features.booking.dto.response;

import com.example.bookingapi.features.booking.model.enums.CancellationPenaltyType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class CancellationPolicyResponse {
    private UUID id;
    private UUID hotelId;
    private String hotelName;
    private String name;
    private String description;
    private Integer freeCancellationHours;
    private CancellationPenaltyType penaltyType;
    private Long penaltyValue;
    private Boolean active;
}

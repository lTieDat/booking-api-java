package com.example.bookingapi.features.receptionist.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class ReceptionistAssignmentResponse {
    private UUID id;
    private UUID userId;
    private String userEmail;
    private String userName;
    private UUID hotelId;
    private String hotelName;
    private Boolean active;
}

package com.example.bookingapi.features.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class GuestResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String middleName;
    private String identifyCardNo;
    private String phoneNumber;
    private String email;
}

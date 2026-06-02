package com.example.bookingapi.features.booking.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingGuestRequest {

    @NotBlank
    @Size(max = 40)
    private String firstName;

    @NotBlank
    @Size(max = 40)
    private String lastName;

    @Size(max = 40)
    private String middleName;

    @NotBlank
    @Size(max = 20)
    private String identifyCardNo;

    @NotBlank
    @Size(max = 20)
    private String phoneNumber;

    @NotBlank
    @Email
    @Size(max = 100)
    private String email;
}

package com.example.bookingapi.features.booking.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(regexp = "^[A-Za-z0-9]{6,20}$", message = "Identity card number must be 6-20 alphanumeric characters")
    @Size(max = 20)
    private String identifyCardNo;

    @NotBlank
    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Phone number must be 8-15 digits and may start with +")
    @Size(max = 20)
    private String phoneNumber;

    @NotBlank
    @Email
    @Size(max = 100)
    private String email;
}

package com.example.bookingapi.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetConfirmRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 4, max = 10)
    private String token;

    @NotBlank
    @Size(min = 6, max = 20)
    private String newPassword;
}


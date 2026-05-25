package com.example.bookingapi.payload.response;

import com.example.bookingapi.model.enums.OtpPurpose;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class OtpTokenResponse {
    private String email;
    private OtpPurpose purpose;
    private String token;
    private Instant expiresAt;

    public OtpTokenResponse(String email, OtpPurpose purpose, String token, Instant expiresAt) {
        this.email = email;
        this.purpose = purpose;
        this.token = token;
        this.expiresAt = expiresAt;
    }
}


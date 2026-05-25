package com.example.bookingapi.payload.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class UserProfile {
    private UUID id;
    private String username;
    private String name;
    private Instant joinedAt;
}

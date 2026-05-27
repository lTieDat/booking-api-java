package com.example.bookingapi.features.room.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class AmenityResponse {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private Integer quantity;
    private Boolean isActive;
}

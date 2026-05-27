package com.example.bookingapi.features.hotel.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class HotelResponse {
    private UUID id;
    private String name;
    private String description;
    private LocationResponse location;
    private HotelImageResponse previewImage;
}

package com.example.bookingapi.features.receptionist.dto.response;

import com.example.bookingapi.features.hotel.dto.response.HotelImageResponse;
import com.example.bookingapi.features.hotel.dto.response.LocationResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class ReceptionistHotelResponse {
    private UUID id;
    private String name;
    private String description;
    private LocationResponse location;
    private HotelImageResponse previewImage;
}

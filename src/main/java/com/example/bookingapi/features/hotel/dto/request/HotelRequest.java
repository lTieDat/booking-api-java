package com.example.bookingapi.features.hotel.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HotelRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    private String description;

    @Valid
    @NotNull
    private LocationRequest location;

    @Valid
    private HotelImageRequest previewImage;
}

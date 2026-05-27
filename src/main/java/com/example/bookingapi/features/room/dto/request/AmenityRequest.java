package com.example.bookingapi.features.room.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AmenityRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 300)
    private String description;

    @Min(1)
    private Integer quantity;

    private Boolean isActive;
}

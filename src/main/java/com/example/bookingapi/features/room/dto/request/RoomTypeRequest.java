package com.example.bookingapi.features.room.dto.request;

import com.example.bookingapi.features.room.model.enums.BedType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RoomTypeRequest {

    @NotBlank
    @Size(max = 50)
    private String name;

    @NotBlank
    @Size(max = 30)
    private String code;

    @NotNull
    @Min(1)
    private Integer maxAdults;

    @NotNull
    @Min(0)
    private Integer maxChildren;

    @NotNull
    @Min(1)
    private Integer maxOccupancy;

    private BedType bedType;

    @Size(max = 500)
    private String description;

    @NotNull
    @Positive
    private BigDecimal basePrice;

    private Boolean isActive;
}

package com.example.bookingapi.features.booking.dto.request;

import com.example.bookingapi.features.booking.model.enums.DiscountType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class DiscountRequest {

    @NotBlank
    @Size(max = 20)
    private String code;

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    private DiscountType discountType;

    @NotNull
    @Min(1)
    private Long discountValue;

    @Min(0)
    private Integer minOrderValue = 0;

    @Min(0)
    private Integer maxOrderValue = 0;

    @NotNull
    private LocalDateTime startDate;

    @NotNull
    private LocalDateTime endDate;

    private Boolean active = false;

    @Min(1)
    private Integer maxUsage;
}

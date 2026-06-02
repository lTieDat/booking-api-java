package com.example.bookingapi.features.booking.dto.response;

import com.example.bookingapi.features.booking.model.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class DiscountResponse {
    private UUID id;
    private String code;
    private String name;
    private DiscountType discountType;
    private Long discountValue;
    private Integer minOrderValue;
    private Integer maxOrderValue;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean active;
    private Integer maxUsage;
    private Integer usedCount;
}

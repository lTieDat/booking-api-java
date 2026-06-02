package com.example.bookingapi.features.hotel.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class LocationResponse {
    private String country;
    private String city;
    private String province;
    private String district;
    private String detail;
    private BigDecimal latitude;
    private BigDecimal longitude;
}

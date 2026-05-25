package com.example.bookingapi.payload.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LocationResponse {
    private String country;
    private String city;
    private String province;
    private String district;
    private String detail;
}

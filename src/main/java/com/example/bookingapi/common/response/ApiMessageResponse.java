package com.example.bookingapi.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ApiMessageResponse {
    private Boolean success;
    private String message;
}

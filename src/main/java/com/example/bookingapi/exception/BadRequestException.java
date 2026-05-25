package com.example.bookingapi.exception;

import com.example.bookingapi.payload.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {

    private final ApiResponse apiResponse;

    public BadRequestException(String message) {
        super(message);
        this.apiResponse = new ApiResponse(false, message);
    }

    public ApiResponse getApiResponse() {
        return apiResponse;
    }
}

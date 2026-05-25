package com.example.bookingapi.exception;

import com.example.bookingapi.payload.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends RuntimeException {

    private final ApiResponse apiResponse;

    public UnauthorizedException(String message) {
        super(message);
        this.apiResponse = new ApiResponse(false, message);
    }

    public ApiResponse getApiResponse() {
        return apiResponse;
    }
}

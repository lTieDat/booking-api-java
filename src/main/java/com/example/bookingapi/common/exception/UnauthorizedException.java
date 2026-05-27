package com.example.bookingapi.common.exception;

import com.example.bookingapi.common.response.ApiMessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends RuntimeException {

    private final ApiMessageResponse apiResponse;

    public UnauthorizedException(String message) {
        super(message);
        this.apiResponse = new ApiMessageResponse(false, message);
    }

    public ApiMessageResponse getApiResponse() {
        return apiResponse;
    }
}

package com.example.bookingapi.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponse {
    private Boolean success = false;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> validationErrors;

    public static ApiErrorResponse of(HttpStatus status, String message, String path) {
        return new ApiErrorResponse(false, status.value(), status.getReasonPhrase(), message, path, null);
    }

    public static ApiErrorResponse validation(String message, String path, Map<String, String> validationErrors) {
        return new ApiErrorResponse(
                false,
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                path,
                validationErrors
        );
    }
}

package com.example.bookingapi.common.exception;

import com.example.bookingapi.common.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.lang.NonNull;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler({
            CannotAcquireLockException.class,
            DeadlockLoserDataAccessException.class,
            PessimisticLockingFailureException.class,
            QueryTimeoutException.class
    })
    public ResponseEntity<ApiErrorResponse> handleConcurrencyConflict(RuntimeException ex, HttpServletRequest request) {
        logger.warn("Concurrency conflict", ex);
        return ResponseEntity.status(HttpStatus.LOCKED)
                .header(HttpHeaders.RETRY_AFTER, "2")
                .body(ApiErrorResponse.of(
                        HttpStatus.LOCKED,
                        "Inventory is being updated. Please retry shortly.",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {
        logger.warn("Database constraint violation", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of(
                        HttpStatus.CONFLICT,
                        "Request conflicts with existing data",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid credentials or account is disabled",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.of(HttpStatus.FORBIDDEN, "Access denied", request.getRequestURI()));
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiErrorResponse> handleAppException(AppException ex, HttpServletRequest request) {
        logger.error("Application error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            @NonNull Exception ex, Object body, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode statusCode, @NonNull WebRequest request) {
        logger.error("Handled MVC exception", ex);
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException ex, @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            errors.put(field, error.getDefaultMessage());
        });
        String path = request.getDescription(false).replace("uri=", "");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.validation("Request validation failed", path, errors));
    }
}

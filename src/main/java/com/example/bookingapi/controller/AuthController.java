package com.example.bookingapi.controller;

import com.example.bookingapi.payload.request.LoginRequest;
import com.example.bookingapi.payload.request.OtpRequest;
import com.example.bookingapi.payload.request.OtpVerifyRequest;
import com.example.bookingapi.payload.request.PasswordResetConfirmRequest;
import com.example.bookingapi.payload.request.SignUpRequest;
import com.example.bookingapi.payload.response.ApiResponse;
import com.example.bookingapi.payload.response.JwtAuthResponse;
import com.example.bookingapi.payload.response.OtpTokenResponse;
import com.example.bookingapi.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Authentication and OTP endpoints")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/signin")
    @Operation(summary = "Sign in as user", description = "Authenticate an existing user and return a JWT access token.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User authenticated successfully.",
                    content = @Content(schema = @Schema(implementation = JwtAuthResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    public ResponseEntity<JwtAuthResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        JwtAuthResponse jwtResponse = authService.signin(loginRequest);
        return ResponseEntity.ok(jwtResponse);
    }

    @PostMapping("/manager/signin")
    @Operation(
            summary = "Sign in as manager",
            description = "Authenticate a manager account and return a JWT access token. "
                    + "Local/dev default manager: email admin@booking.local, password admin123."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Manager authenticated successfully.",
                    content = @Content(schema = @Schema(implementation = JwtAuthResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    public ResponseEntity<JwtAuthResponse> authenticateManager(@Valid @RequestBody LoginRequest loginRequest) {
        JwtAuthResponse jwtResponse = authService.signin(loginRequest);
        return ResponseEntity.ok(jwtResponse);
    }

    @PostMapping("/signup")
    @Operation(summary = "Sign up user", description = "Create a new user account with default role assignment.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered successfully.",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    public ResponseEntity<ApiResponse> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) {
        ApiResponse apiResponse = authService.signup(signUpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @PostMapping("/otp/email-verification/request")
    @Operation(summary = "Request email verification OTP", description = "Generate an OTP token for verifying a newly registered email.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Email verification OTP generated successfully.",
                    content = @Content(schema = @Schema(implementation = OtpTokenResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    public ResponseEntity<OtpTokenResponse> requestEmailVerification(@Valid @RequestBody OtpRequest otpRequest) {
        return ResponseEntity.ok(authService.requestEmailVerification(otpRequest));
    }

    @PostMapping("/otp/email-verification/confirm")
    @Operation(summary = "Confirm email verification OTP", description = "Confirm the OTP token and mark the email as verified.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Email verified successfully.",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    public ResponseEntity<ApiResponse> verifyEmail(@Valid @RequestBody OtpVerifyRequest otpVerifyRequest) {
        return ResponseEntity.ok(authService.verifyEmail(otpVerifyRequest));
    }

    @PostMapping("/otp/password-reset/request")
    @Operation(summary = "Request password reset OTP", description = "Generate an OTP token for resetting a password.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password reset OTP generated successfully.",
                    content = @Content(schema = @Schema(implementation = OtpTokenResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    public ResponseEntity<OtpTokenResponse> requestPasswordReset(@Valid @RequestBody OtpRequest otpRequest) {
        return ResponseEntity.ok(authService.requestPasswordReset(otpRequest));
    }

    @PostMapping("/otp/password-reset/confirm")
    @Operation(summary = "Confirm password reset OTP", description = "Confirm the OTP token and update the account password.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password reset successfully.",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    public ResponseEntity<ApiResponse> resetPassword(@Valid @RequestBody PasswordResetConfirmRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }
}

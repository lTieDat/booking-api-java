package com.example.bookingapi.service;

import com.example.bookingapi.payload.request.LoginRequest;
import com.example.bookingapi.payload.request.OtpRequest;
import com.example.bookingapi.payload.request.OtpVerifyRequest;
import com.example.bookingapi.payload.request.PasswordResetConfirmRequest;
import com.example.bookingapi.payload.request.SignUpRequest;
import com.example.bookingapi.payload.response.ApiResponse;
import com.example.bookingapi.payload.response.OtpTokenResponse;
import com.example.bookingapi.payload.response.JwtAuthResponse;

public interface AuthService {
    JwtAuthResponse signin(LoginRequest loginRequest);
    OtpTokenResponse requestEmailVerification(OtpRequest otpRequest);
    ApiResponse verifyEmail(OtpVerifyRequest otpVerifyRequest);
    OtpTokenResponse requestPasswordReset(OtpRequest otpRequest);
    ApiResponse resetPassword(PasswordResetConfirmRequest request);
    ApiResponse signup(SignUpRequest signUpRequest);
}

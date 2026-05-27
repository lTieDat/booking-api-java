package com.example.bookingapi.features.auth.service;

import com.example.bookingapi.features.auth.dto.request.LoginRequest;
import com.example.bookingapi.features.auth.dto.request.OtpRequest;
import com.example.bookingapi.features.auth.dto.request.OtpVerifyRequest;
import com.example.bookingapi.features.auth.dto.request.PasswordResetConfirmRequest;
import com.example.bookingapi.features.auth.dto.request.SignUpRequest;
import com.example.bookingapi.common.response.ApiMessageResponse;
import com.example.bookingapi.features.auth.dto.response.OtpTokenResponse;
import com.example.bookingapi.features.auth.dto.response.JwtAuthResponse;

public interface AuthService {
    JwtAuthResponse signin(LoginRequest loginRequest);
    OtpTokenResponse requestEmailVerification(OtpRequest otpRequest);
    ApiMessageResponse verifyEmail(OtpVerifyRequest otpVerifyRequest);
    OtpTokenResponse requestPasswordReset(OtpRequest otpRequest);
    ApiMessageResponse resetPassword(PasswordResetConfirmRequest request);
    ApiMessageResponse signup(SignUpRequest signUpRequest);
}

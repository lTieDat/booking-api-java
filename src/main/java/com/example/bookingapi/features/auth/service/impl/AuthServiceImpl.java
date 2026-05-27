package com.example.bookingapi.features.auth.service.impl;

import com.example.bookingapi.common.exception.AppException;
import com.example.bookingapi.common.exception.BadRequestException;
import com.example.bookingapi.common.exception.ResourceNotFoundException;
import com.example.bookingapi.features.auth.model.Role;
import com.example.bookingapi.features.auth.model.OTPToken;
import com.example.bookingapi.features.user.model.User;
import com.example.bookingapi.features.auth.model.enums.OtpPurpose;
import com.example.bookingapi.features.auth.model.enums.RoleName;
import com.example.bookingapi.features.auth.dto.request.LoginRequest;
import com.example.bookingapi.features.auth.dto.request.OtpRequest;
import com.example.bookingapi.features.auth.dto.request.OtpVerifyRequest;
import com.example.bookingapi.features.auth.dto.request.PasswordResetConfirmRequest;
import com.example.bookingapi.features.auth.dto.request.SignUpRequest;
import com.example.bookingapi.common.response.ApiMessageResponse;
import com.example.bookingapi.features.auth.dto.response.JwtAuthResponse;
import com.example.bookingapi.features.auth.dto.response.OtpTokenResponse;
import com.example.bookingapi.features.auth.repository.ManagerRepository;
import com.example.bookingapi.features.auth.repository.OTPTokenRepository;
import com.example.bookingapi.features.auth.repository.RoleRepository;
import com.example.bookingapi.features.user.repository.UserRepository;
import com.example.bookingapi.common.security.JwtTokenProvider;
import com.example.bookingapi.features.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private UserRepository userRepository;
    @Autowired private ManagerRepository managerRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private OTPTokenRepository otpTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider tokenProvider;

    @Override
    public JwtAuthResponse signin(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmail(),
                        loginRequest.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);
        return new JwtAuthResponse(jwt);
    }

    @Override
    @Transactional
    public OtpTokenResponse requestEmailVerification(OtpRequest otpRequest) {
        User user = getUserByEmail(otpRequest.getEmail());
        if (Boolean.TRUE.equals(user.getIsVerified())) {
            throw new BadRequestException("User is already verified");
        }

        deactivateExistingOtps(user.getId(), OtpPurpose.EMAIL_VERIFICATION);
        OTPToken token = createOtpToken(user, OtpPurpose.EMAIL_VERIFICATION);
        return new OtpTokenResponse(user.getEmail(), token.getPurpose(), token.getToken(), token.getExpiresAt());
    }

    @Override
    @Transactional
    public ApiMessageResponse verifyEmail(OtpVerifyRequest otpVerifyRequest) {
        User user = getUserByEmail(otpVerifyRequest.getEmail());
        OTPToken token = otpTokenRepository.findByUser_IdAndPurposeAndTokenAndIsUsedFalse(
                        user.getId(), OtpPurpose.EMAIL_VERIFICATION, otpVerifyRequest.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid verification token"));

        validateOtpExpiration(token);
        user.setIsVerified(true);
        token.setIsUsed(true);
        return new ApiMessageResponse(true, "Email verified successfully");
    }

    @Override
    @Transactional
    public OtpTokenResponse requestPasswordReset(OtpRequest otpRequest) {
        User user = getUserByEmail(otpRequest.getEmail());
        deactivateExistingOtps(user.getId(), OtpPurpose.PASSWORD_RESET);
        OTPToken token = createOtpToken(user, OtpPurpose.PASSWORD_RESET);
        return new OtpTokenResponse(user.getEmail(), token.getPurpose(), token.getToken(), token.getExpiresAt());
    }

    @Override
    @Transactional
    public ApiMessageResponse resetPassword(PasswordResetConfirmRequest request) {
        User user = getUserByEmail(request.getEmail());
        OTPToken token = otpTokenRepository.findByUser_IdAndPurposeAndTokenAndIsUsedFalse(
                        user.getId(), OtpPurpose.PASSWORD_RESET, request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid password reset token"));

        validateOtpExpiration(token);
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        token.setIsUsed(true);
        return new ApiMessageResponse(true, "Password reset successfully");
    }

    @Override
    @Transactional
    public ApiMessageResponse signup(SignUpRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new BadRequestException("Username is already taken");
        }
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new BadRequestException("Email address is already in use");
        }
        if (managerRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new BadRequestException("Email address is already in use");
        }

        User user = new User();
        user.setName(signUpRequest.getName());
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        user.setIsVerified(false);

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new AppException("User role not set. Please contact admin."));
        user.setRoles(Collections.singleton(userRole));
        userRepository.save(user);

        return new ApiMessageResponse(true, "User registered successfully. Please verify your email before sign in.");
    }

    private User getUserByEmail(String email) {
        return userRepository.findByUsernameOrEmail(email, email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private void deactivateExistingOtps(UUID userId, OtpPurpose purpose) {
        List<OTPToken> activeTokens = otpTokenRepository.findByUser_IdAndPurposeAndIsUsedFalse(userId, purpose);
        activeTokens.forEach(token -> token.setIsUsed(true));
    }

    private OTPToken createOtpToken(User user, OtpPurpose purpose) {
        OTPToken otpToken = new OTPToken();
        otpToken.setUser(user);
        otpToken.setPurpose(purpose);
        otpToken.setToken(generateOtpToken());
        otpToken.setExpiresAt(Instant.now().plus(Duration.ofMinutes(10)));
        otpToken.setIsUsed(false);
        return otpTokenRepository.save(otpToken);
    }

    private void validateOtpExpiration(OTPToken token) {
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("OTP token has expired");
        }
    }

    private String generateOtpToken() {
        int value = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(value);
    }
}

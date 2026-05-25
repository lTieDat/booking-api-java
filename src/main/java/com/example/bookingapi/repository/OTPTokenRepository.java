package com.example.bookingapi.repository;

import com.example.bookingapi.model.OTPToken;
import com.example.bookingapi.model.enums.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OTPTokenRepository extends JpaRepository<OTPToken, UUID> {
    List<OTPToken> findByUser_IdAndPurposeAndIsUsedFalse(UUID userId, OtpPurpose purpose);
    Optional<OTPToken> findByUser_IdAndPurposeAndTokenAndIsUsedFalse(UUID userId, OtpPurpose purpose, String token);
    List<OTPToken> findByIsUsedFalseAndExpiresAtBefore(Instant expiresAt);
}


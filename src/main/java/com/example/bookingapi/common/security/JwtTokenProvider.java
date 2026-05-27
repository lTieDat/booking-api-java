package com.example.bookingapi.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import com.example.bookingapi.features.auth.model.enums.ActorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationInMs}")
    private int jwtExpirationInMs;

    /**
     * Create a signed JWT for the authenticated user.
     *
     * The token subject is the internal user id so the filter can reload
     * the user from the database on every request.
     */
    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        UUID principalId = userPrincipal.getId();
        UUID userId = Objects.requireNonNull(principalId, "Authenticated user id cannot be null");
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);
        return Jwts.builder()
                .subject(userId.toString())
                .claim("actorType", userPrincipal.getActorType().name())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extract the authenticated user's id from a valid JWT.
     */
    public UUID getUserIdFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return UUID.fromString(claims.getSubject());
    }

    public ActorType getActorTypeFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        String actorType = claims.get("actorType", String.class);
        if (actorType == null) {
            return ActorType.USER;
        }
        try {
            return ActorType.valueOf(actorType);
        } catch (IllegalArgumentException ex) {
            logger.error("Invalid actor type in JWT: {}", actorType);
            return ActorType.USER;
        }
    }

    /**
     * Validate signature, expiration and token format.
     */
    public boolean validateToken(String authToken) {
        if (authToken == null || authToken.isBlank()) {
            logger.error("JWT token is empty");
            return false;
        }
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(authToken);
            return true;
        } catch (io.jsonwebtoken.security.SignatureException ex) {
            logger.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty");
        }
        return false;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}

package com.example.bookingapi.features.booking.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class BookingIdempotencyCacheService {

    private static final String KEY_PREFIX = "booking:create:";

    private final StringRedisTemplate redisTemplate;
    private final Duration processingTtl;
    private final Duration completedTtl;

    public BookingIdempotencyCacheService(
            StringRedisTemplate redisTemplate,
            @Value("${app.booking.idempotency.processing-ttl-seconds:900}") long processingTtlSeconds,
            @Value("${app.booking.idempotency.completed-ttl-seconds:86400}") long completedTtlSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.processingTtl = Duration.ofSeconds(processingTtlSeconds);
        this.completedTtl = Duration.ofSeconds(completedTtlSeconds);
    }

    public Optional<BookingIdempotencyState> find(UUID userId, String clientRequestId) {
        String value = redisTemplate.opsForValue().get(key(userId, clientRequestId));
        return value == null ? Optional.empty() : Optional.of(BookingIdempotencyState.deserialize(value));
    }

    public boolean putProcessingIfAbsent(UUID userId, String clientRequestId, String requestHash) {
        Boolean inserted = redisTemplate.opsForValue().setIfAbsent(
                key(userId, clientRequestId),
                BookingIdempotencyState.processing(requestHash).serialize(),
                processingTtl
        );
        return Boolean.TRUE.equals(inserted);
    }

    public void putCompleted(UUID userId, String clientRequestId, String requestHash, UUID bookingId) {
        redisTemplate.opsForValue().set(
                key(userId, clientRequestId),
                BookingIdempotencyState.completed(requestHash, bookingId).serialize(),
                completedTtl
        );
    }

    public void delete(UUID userId, String clientRequestId) {
        redisTemplate.delete(key(userId, clientRequestId));
    }

    private String key(UUID userId, String clientRequestId) {
        return KEY_PREFIX + userId + ":" + clientRequestId;
    }
}

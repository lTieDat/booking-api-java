package com.example.bookingapi.features.booking.job;

import com.example.bookingapi.features.booking.service.BookingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(
        name = "app.jobs.booking-expiration.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class BookingExpirationJob {

    private static final String JOB_NAME = "booking-expiration";

    private final BookingService bookingService;

    public BookingExpirationJob(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(fixedDelayString = "${app.jobs.booking-expiration.fixed-delay-ms:60000}")
    public void expirePendingBookings() {
        long startedAt = System.currentTimeMillis();
        log.info("Job started: jobName={}", JOB_NAME);
        try {
            int expiredCount = bookingService.expirePendingBookings();
            long durationMs = System.currentTimeMillis() - startedAt;
            log.info("Job completed: jobName={} expiredCount={} durationMs={}", JOB_NAME, expiredCount, durationMs);
        } catch (Exception ex) {
            long durationMs = System.currentTimeMillis() - startedAt;
            log.error("Job failed: jobName={} durationMs={}", JOB_NAME, durationMs, ex);
            throw ex;
        }
    }
}

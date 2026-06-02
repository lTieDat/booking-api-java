package com.example.bookingapi.features.payment.job;

import com.example.bookingapi.features.payment.service.PaymentReconciliationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(
        name = "app.jobs.payment-reconciliation.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PaymentReconciliationJob {

    private static final String JOB_NAME = "payment-reconciliation";

    private final PaymentReconciliationService paymentReconciliationService;

    public PaymentReconciliationJob(PaymentReconciliationService paymentReconciliationService) {
        this.paymentReconciliationService = paymentReconciliationService;
    }

    @Scheduled(fixedDelayString = "${app.jobs.payment-reconciliation.fixed-delay-ms:300000}")
    public void reconcilePayments() {
        long startedAt = System.currentTimeMillis();
        log.info("Job started: jobName={}", JOB_NAME);
        try {
            int reconciledCount = paymentReconciliationService.reconcileExpiredOrPendingPayments();
            long durationMs = System.currentTimeMillis() - startedAt;
            log.info("Job completed: jobName={} reconciledCount={} durationMs={}", JOB_NAME, reconciledCount, durationMs);
        } catch (Exception ex) {
            long durationMs = System.currentTimeMillis() - startedAt;
            log.error("Job failed: jobName={} durationMs={}", JOB_NAME, durationMs, ex);
        }
    }
}

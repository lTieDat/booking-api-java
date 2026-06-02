package com.example.bookingapi.features.payment.model;

import com.example.bookingapi.features.payment.model.enums.PaymentProvider;
import com.example.bookingapi.features.payment.model.enums.PaymentWebhookStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "payment_webhook_events",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_webhook_events_provider_payload_hash",
                        columnNames = {"provider", "payload_hash"}
                ),
                @UniqueConstraint(
                        name = "uk_payment_webhook_events_provider_event",
                        columnNames = {"provider", "provider_event_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class PaymentWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 30)
    private PaymentProvider provider = PaymentProvider.PAYOS;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "provider_event_id", length = 150)
    private String providerEventId;

    @Column(name = "provider_order_code", length = 100)
    private String providerOrderCode;

    @Column(name = "provider_payment_id", length = 150)
    private String providerPaymentId;

    @Column(name = "event_type", length = 80)
    private String eventType;

    @Column(name = "signature", length = 500)
    private String signature;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentWebhookStatus status = PaymentWebhookStatus.RECEIVED;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;
}

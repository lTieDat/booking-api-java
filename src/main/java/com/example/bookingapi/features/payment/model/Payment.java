package com.example.bookingapi.features.payment.model;

import com.example.bookingapi.common.audit.DateAudit;
import com.example.bookingapi.features.booking.model.Booking;
import com.example.bookingapi.features.payment.model.enums.PaymentProvider;
import com.example.bookingapi.features.payment.model.enums.PaymentStatus;
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
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payments_booking_attempt", columnNames = {"booking_id", "attempt_no"}),
                @UniqueConstraint(name = "uk_payments_provider_order_code", columnNames = {"provider", "provider_order_code"}),
                @UniqueConstraint(name = "uk_payments_provider_payment_id", columnNames = {"provider", "provider_payment_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Payment extends DateAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_account_id")
    private PaymentProviderAccount providerAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 30)
    private PaymentProvider provider = PaymentProvider.PAYOS;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentStatus status = PaymentStatus.INITIATED;

    @Column(name = "provider_status", length = 40)
    private String providerStatus;

    @Column(name = "amount_minor", nullable = false)
    private Long amountMinor;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "VND";

    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo = 1;

    @Column(name = "provider_order_code", length = 100)
    private String providerOrderCode;

    @Column(name = "provider_payment_id", length = 150)
    private String providerPaymentId;

    @Column(name = "provider_reference", length = 150)
    private String providerReference;

    @Column(name = "checkout_url", length = 500)
    private String checkoutUrl;

    @Column(name = "qr_code", columnDefinition = "TEXT")
    private String qrCode;

    @Column(name = "return_url", length = 500)
    private String returnUrl;

    @Column(name = "cancel_url", length = 500)
    private String cancelUrl;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "last_reconciled_at")
    private LocalDateTime lastReconciledAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;
}

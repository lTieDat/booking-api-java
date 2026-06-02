package com.example.bookingapi.features.payment.model;

import com.example.bookingapi.common.audit.DateAudit;
import com.example.bookingapi.features.payment.model.enums.PaymentProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "payment_provider_accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_provider_accounts_provider_mode_merchant",
                        columnNames = {"provider", "mode", "merchant_code"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class PaymentProviderAccount extends DateAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 30)
    private PaymentProvider provider = PaymentProvider.PAYOS;

    @Column(name = "mode", nullable = false, length = 10)
    private String mode = "TEST";

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "merchant_code", length = 100)
    private String merchantCode;

    @Column(name = "provider_channel_id", length = 100)
    private String providerChannelId;

    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;

    @Column(name = "webhook_confirmed_at")
    private LocalDateTime webhookConfirmedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;
}

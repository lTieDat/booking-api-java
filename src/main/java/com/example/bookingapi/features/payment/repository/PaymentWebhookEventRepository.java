package com.example.bookingapi.features.payment.repository;

import com.example.bookingapi.features.payment.model.PaymentWebhookEvent;
import com.example.bookingapi.features.payment.model.enums.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, UUID> {
    boolean existsByProviderAndPayloadHash(PaymentProvider provider, String payloadHash);

    Optional<PaymentWebhookEvent> findByProviderAndPayloadHash(PaymentProvider provider, String payloadHash);
}

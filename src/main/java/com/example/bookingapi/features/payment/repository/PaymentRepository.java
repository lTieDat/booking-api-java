package com.example.bookingapi.features.payment.repository;

import com.example.bookingapi.features.payment.model.Payment;
import com.example.bookingapi.features.payment.model.enums.PaymentProvider;
import com.example.bookingapi.features.payment.model.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByBooking_IdAndProviderAndStatusIn(
            UUID bookingId,
            PaymentProvider provider,
            Collection<PaymentStatus> statuses
    );

    Optional<Payment> findFirstByBooking_IdAndProviderAndStatusInOrderByAttemptNoDesc(
            UUID bookingId,
            PaymentProvider provider,
            Collection<PaymentStatus> statuses
    );

    Optional<Payment> findFirstByBooking_IdAndProviderOrderByAttemptNoDesc(
            UUID bookingId,
            PaymentProvider provider
    );

    Optional<Payment> findByProviderAndProviderOrderCode(
            PaymentProvider provider,
            String providerOrderCode
    );

    Optional<Payment> findByProviderAndProviderPaymentId(
            PaymentProvider provider,
            String providerPaymentId
    );

    List<Payment> findByStatusInAndExpiresAtLessThanEqual(
            Collection<PaymentStatus> statuses,
            LocalDateTime expiresAt
    );
}

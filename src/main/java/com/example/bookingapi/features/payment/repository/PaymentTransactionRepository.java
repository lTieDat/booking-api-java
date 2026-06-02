package com.example.bookingapi.features.payment.repository;

import com.example.bookingapi.features.payment.model.PaymentTransaction;
import com.example.bookingapi.features.payment.model.enums.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    Optional<PaymentTransaction> findByProviderAndProviderTransactionId(
            PaymentProvider provider,
            String providerTransactionId
    );

    boolean existsByProviderAndProviderTransactionId(PaymentProvider provider, String providerTransactionId);
}

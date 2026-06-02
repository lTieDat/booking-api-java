package com.example.bookingapi.features.payment.repository;

import com.example.bookingapi.features.payment.model.PaymentProviderAccount;
import com.example.bookingapi.features.payment.model.enums.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentProviderAccountRepository extends JpaRepository<PaymentProviderAccount, UUID> {
    Optional<PaymentProviderAccount> findByProviderAndModeAndMerchantCode(
            PaymentProvider provider,
            String mode,
            String merchantCode
    );
}

package com.example.bookingapi.features.payment.service.impl;

import com.example.bookingapi.features.booking.model.enums.BookingStatus;
import com.example.bookingapi.features.payment.model.Payment;
import com.example.bookingapi.features.payment.model.enums.PaymentStatus;
import com.example.bookingapi.features.payment.repository.PaymentRepository;
import com.example.bookingapi.features.payment.service.PaymentReconciliationService;
import com.example.bookingapi.features.payment.service.PayosPaymentClient;
import com.example.bookingapi.features.payment.service.PayosPaymentLinkResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentReconciliationServiceImpl implements PaymentReconciliationService {

    private static final List<PaymentStatus> RECONCILABLE_STATUSES = List.of(
            PaymentStatus.INITIATED,
            PaymentStatus.PENDING
    );

    private final PaymentRepository paymentRepository;
    private final PayosPaymentClient payosPaymentClient;
    private final PayosSettlementSupport settlementSupport;

    public PaymentReconciliationServiceImpl(
            PaymentRepository paymentRepository,
            PayosPaymentClient payosPaymentClient,
            PayosSettlementSupport settlementSupport
    ) {
        this.paymentRepository = paymentRepository;
        this.payosPaymentClient = payosPaymentClient;
        this.settlementSupport = settlementSupport;
    }

    @Override
    @Transactional
    public int reconcileExpiredOrPendingPayments() {
        List<Payment> payments = paymentRepository.findByStatusInAndExpiresAtLessThanEqual(
                RECONCILABLE_STATUSES,
                LocalDateTime.now()
        );
        for (Payment payment : payments) {
            reconcile(payment);
        }
        return payments.size();
    }

    private void reconcile(Payment payment) {
        if (payment.getProviderOrderCode() == null || payment.getProviderOrderCode().isBlank()) {
            settlementSupport.expireOrCancel(
                    payment,
                    PaymentStatus.EXPIRED,
                    BookingStatus.EXPIRED,
                    "Payment expired before provider order was created"
            );
            return;
        }
        try {
            PayosPaymentLinkResult link = payosPaymentClient.getPaymentLink(payment.getProviderOrderCode());
            settlementSupport.settlePaymentLink(payment, link, String.valueOf(link));
        } catch (RuntimeException ex) {
            settlementSupport.expireOrCancel(
                    payment,
                    PaymentStatus.EXPIRED,
                    BookingStatus.EXPIRED,
                    "Payment expired before provider settlement could be confirmed"
            );
        }
    }
}

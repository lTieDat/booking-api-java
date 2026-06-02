package com.example.bookingapi.features.payment.service.impl;

import com.example.bookingapi.features.booking.model.enums.BookingStatus;
import com.example.bookingapi.features.booking.service.BookingPaymentService;
import com.example.bookingapi.features.invoice.service.InvoiceService;
import com.example.bookingapi.features.payment.model.Payment;
import com.example.bookingapi.features.payment.model.PaymentTransaction;
import com.example.bookingapi.features.payment.model.enums.PaymentProvider;
import com.example.bookingapi.features.payment.model.enums.PaymentStatus;
import com.example.bookingapi.features.payment.repository.PaymentRepository;
import com.example.bookingapi.features.payment.repository.PaymentTransactionRepository;
import com.example.bookingapi.features.payment.service.PayosPaymentLinkResult;
import com.example.bookingapi.features.payment.service.PayosPaymentLinkTransaction;
import com.example.bookingapi.features.payment.service.PayosWebhookVerificationResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class PayosSettlementSupport {

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final BookingPaymentService bookingPaymentService;
    private final InvoiceService invoiceService;

    public PayosSettlementSupport(
            PaymentRepository paymentRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            BookingPaymentService bookingPaymentService,
            InvoiceService invoiceService
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.bookingPaymentService = bookingPaymentService;
        this.invoiceService = invoiceService;
    }

    public void settlePaidWebhook(Payment payment, PayosWebhookVerificationResult data, String rawPayload) {
        if (payment.getStatus() == PaymentStatus.PAID) {
            return;
        }
        if (data.reference() != null
                && paymentTransactionRepository.existsByProviderAndProviderTransactionId(PaymentProvider.PAYOS, data.reference())) {
            payment.setStatus(PaymentStatus.PAID);
            paymentRepository.save(payment);
            return;
        }
        payment.setProviderStatus(resolveProviderStatus(data.code(), "PAID"));
        payment.setProviderReference(data.reference());
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);
        saveTransaction(payment, data.reference(), data.amountMinor(), data.description(), data.transactionDateTime(), rawPayload);
        bookingPaymentService.confirmPaidBooking(payment.getBooking().getId(), payment.getId());
        invoiceService.generatePaidInvoiceForPayment(payment);
    }

    public void settlePaymentLink(Payment payment, PayosPaymentLinkResult link, String rawPayload) {
        String status = link.status();
        payment.setProviderStatus(status);
        payment.setLastReconciledAt(LocalDateTime.now());
        if ("PAID".equals(status)) {
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);
            Optional<PayosPaymentLinkTransaction> firstTransaction = link.transactions().stream().findFirst();
            firstTransaction.ifPresent(transaction -> {
                if (transaction.reference() == null
                        || !paymentTransactionRepository.existsByProviderAndProviderTransactionId(
                        PaymentProvider.PAYOS, transaction.reference())) {
                    saveTransaction(
                            payment,
                            transaction.reference(),
                            transaction.amountMinor(),
                            transaction.description(),
                            transaction.transactionDateTime(),
                            rawPayload
                    );
                }
            });
            bookingPaymentService.confirmPaidBooking(payment.getBooking().getId(), payment.getId());
            invoiceService.generatePaidInvoiceForPayment(payment);
            return;
        }
        if ("CANCELLED".equals(status)) {
            expireOrCancel(payment, PaymentStatus.CANCELLED, BookingStatus.CANCELLED, "payOS payment cancelled");
            return;
        }
        if ("EXPIRED".equals(status)) {
            expireOrCancel(payment, PaymentStatus.EXPIRED, BookingStatus.EXPIRED, "payOS payment expired");
        } else {
            paymentRepository.save(payment);
        }
    }

    public void expireOrCancel(Payment payment, PaymentStatus paymentStatus, BookingStatus bookingStatus, String reason) {
        if (payment.getStatus() == PaymentStatus.PAID) {
            return;
        }
        payment.setStatus(paymentStatus);
        if (paymentStatus == PaymentStatus.CANCELLED) {
            payment.setCancelledAt(LocalDateTime.now());
        }
        paymentRepository.save(payment);
        bookingPaymentService.releaseUnpaidBooking(payment.getBooking().getId(), payment.getId(), bookingStatus, reason);
    }

    private void saveTransaction(
            Payment payment,
            String reference,
            Long amountMinor,
            String description,
            String transactionDateTime,
            String rawPayload
    ) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setPayment(payment);
        transaction.setProvider(PaymentProvider.PAYOS);
        transaction.setProviderTransactionId(reference);
        transaction.setProviderOrderCode(payment.getProviderOrderCode());
        transaction.setProviderPaymentId(payment.getProviderPaymentId());
        transaction.setAmountMinor(amountMinor == null ? payment.getAmountMinor() : amountMinor);
        transaction.setCurrency(payment.getCurrency());
        transaction.setDescription(description);
        transaction.setTransactionAt(LocalDateTime.now());
        transaction.setRawPayload(rawPayload);
        paymentTransactionRepository.save(transaction);
    }

    private String resolveProviderStatus(String code, String fallback) {
        if (code == null || code.isBlank()) {
            return fallback;
        }
        return code;
    }
}

package com.example.bookingapi.features.payment.service.impl;

import com.example.bookingapi.common.exception.BadRequestException;
import com.example.bookingapi.common.exception.ResourceNotFoundException;
import com.example.bookingapi.features.payment.model.Payment;
import com.example.bookingapi.features.payment.model.PaymentWebhookEvent;
import com.example.bookingapi.features.payment.model.enums.PaymentProvider;
import com.example.bookingapi.features.payment.model.enums.PaymentWebhookStatus;
import com.example.bookingapi.features.payment.repository.PaymentRepository;
import com.example.bookingapi.features.payment.repository.PaymentWebhookEventRepository;
import com.example.bookingapi.features.payment.service.PayosPaymentClient;
import com.example.bookingapi.features.payment.service.PayosWebhookService;
import com.example.bookingapi.features.payment.service.PayosWebhookVerificationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.model.webhooks.Webhook;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class PayosWebhookServiceImpl implements PayosWebhookService {

    private final PayosPaymentClient payosPaymentClient;
    private final PaymentRepository paymentRepository;
    private final PaymentWebhookEventRepository webhookEventRepository;
    private final PayosSettlementSupport settlementSupport;

    public PayosWebhookServiceImpl(
            PayosPaymentClient payosPaymentClient,
            PaymentRepository paymentRepository,
            PaymentWebhookEventRepository webhookEventRepository,
            PayosSettlementSupport settlementSupport
    ) {
        this.payosPaymentClient = payosPaymentClient;
        this.paymentRepository = paymentRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.settlementSupport = settlementSupport;
    }

    @Override
    @Transactional(noRollbackFor = BadRequestException.class)
    public void handleWebhook(Webhook webhook) {
        String rawPayload = String.valueOf(webhook);
        String payloadHash = sha256(rawPayload);
        if (webhookEventRepository.existsByProviderAndPayloadHash(PaymentProvider.PAYOS, payloadHash)) {
            return;
        }

        PaymentWebhookEvent event = new PaymentWebhookEvent();
        event.setProvider(PaymentProvider.PAYOS);
        event.setPayload(rawPayload);
        event.setPayloadHash(payloadHash);
        event.setSignature(webhook.getSignature());
        event.setStatus(PaymentWebhookStatus.RECEIVED);
        event.setReceivedAt(LocalDateTime.now());
        webhookEventRepository.saveAndFlush(event);

        PayosWebhookVerificationResult data;
        try {
            data = payosPaymentClient.verifyWebhook(webhook);
        } catch (BadRequestException ex) {
            event.setStatus(PaymentWebhookStatus.FAILED);
            event.setErrorMessage(ex.getMessage());
            event.setProcessedAt(LocalDateTime.now());
            webhookEventRepository.save(event);
            throw ex;
        }
        event.setVerifiedAt(LocalDateTime.now());
        event.setProviderOrderCode(data.orderCode() == null ? null : String.valueOf(data.orderCode()));
        event.setProviderPaymentId(data.paymentLinkId());
        event.setProviderEventId(data.reference());
        event.setEventType(resolveEventType(data));

        Payment payment = resolvePayment(data);
        if (payment == null) {
            event.setStatus(PaymentWebhookStatus.IGNORED);
            event.setProcessedAt(LocalDateTime.now());
            webhookEventRepository.save(event);
            return;
        }

        event.setPayment(payment);
        if (isPaid(data)) {
            settlementSupport.settlePaidWebhook(payment, data, rawPayload);
            event.setStatus(PaymentWebhookStatus.PROCESSED);
        } else {
            event.setStatus(PaymentWebhookStatus.IGNORED);
        }
        event.setProcessedAt(LocalDateTime.now());
        webhookEventRepository.save(event);
    }

    private Payment resolvePayment(PayosWebhookVerificationResult data) {
        if (data.orderCode() != null) {
            return paymentRepository
                    .findByProviderAndProviderOrderCode(PaymentProvider.PAYOS, String.valueOf(data.orderCode()))
                    .orElse(null);
        }
        if (data.paymentLinkId() != null) {
            return paymentRepository
                    .findByProviderAndProviderPaymentId(PaymentProvider.PAYOS, data.paymentLinkId())
                    .orElse(null);
        }
        return null;
    }

    private boolean isPaid(PayosWebhookVerificationResult data) {
        return data.orderCode() != null && data.amountMinor() != null && data.reference() != null;
    }

    private String resolveEventType(PayosWebhookVerificationResult data) {
        if (isPaid(data)) {
            return "PAID";
        }
        return data.code() == null ? "UNKNOWN" : data.code();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new ResourceNotFoundException("SHA-256", "algorithm", "SHA-256");
        }
    }
}

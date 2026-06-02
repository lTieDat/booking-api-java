package com.example.bookingapi.features.payment.service;

public interface PayosPaymentClient {
    void ensureAvailable();
    PayosCreatePaymentResult createPaymentLink(PayosCreatePaymentCommand command);
    PayosWebhookVerificationResult verifyWebhook(Object webhook);
    PayosPaymentLinkResult getPaymentLink(String id);
    void cancelPaymentLink(String id, String reason);
}

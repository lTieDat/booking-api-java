package com.example.bookingapi.features.payment.service;

import vn.payos.model.webhooks.Webhook;

public interface PayosWebhookService {
    void handleWebhook(Webhook webhook);
}

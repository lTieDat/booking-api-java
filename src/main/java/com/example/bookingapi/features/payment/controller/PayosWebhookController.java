package com.example.bookingapi.features.payment.controller;

import com.example.bookingapi.features.payment.service.PayosWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.payos.model.webhooks.Webhook;

@RestController
@RequestMapping("/api/payments/payos")
@Tag(name = "payOS Webhooks", description = "Public payOS webhook endpoint.")
public class PayosWebhookController {

    private final PayosWebhookService payosWebhookService;

    public PayosWebhookController(PayosWebhookService payosWebhookService) {
        this.payosWebhookService = payosWebhookService;
    }

    @PostMapping("/webhook")
    @Operation(
            summary = "Receive payOS webhook",
            description = "Verify and process payOS payment webhook. This endpoint is public but signature-verified."
    )
    public ResponseEntity<Void> handleWebhook(@RequestBody Webhook webhook) {
        payosWebhookService.handleWebhook(webhook);
        return ResponseEntity.ok().build();
    }
}

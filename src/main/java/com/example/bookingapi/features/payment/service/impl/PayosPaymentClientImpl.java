package com.example.bookingapi.features.payment.service.impl;

import com.example.bookingapi.common.config.PayosProperties;
import com.example.bookingapi.common.exception.BadRequestException;
import com.example.bookingapi.features.payment.exception.PaymentProviderException;
import com.example.bookingapi.features.payment.service.PayosCreatePaymentCommand;
import com.example.bookingapi.features.payment.service.PayosCreatePaymentItem;
import com.example.bookingapi.features.payment.service.PayosCreatePaymentResult;
import com.example.bookingapi.features.payment.service.PayosPaymentLinkResult;
import com.example.bookingapi.features.payment.service.PayosPaymentLinkTransaction;
import com.example.bookingapi.features.payment.service.PayosPaymentClient;
import com.example.bookingapi.features.payment.service.PayosWebhookVerificationResult;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import vn.payos.PayOS;
import vn.payos.model.webhooks.WebhookData;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;
import vn.payos.model.v2.paymentRequests.Transaction;

import java.time.ZoneId;
import java.util.List;

@Component
public class PayosPaymentClientImpl implements PayosPaymentClient {

    private final ObjectProvider<PayOS> payOSProvider;
    private final PayosProperties properties;

    public PayosPaymentClientImpl(ObjectProvider<PayOS> payOSProvider, PayosProperties properties) {
        this.payOSProvider = payOSProvider;
        this.properties = properties;
    }

    @Override
    public void ensureAvailable() {
        if (!properties.isEnabled()) {
            throw new BadRequestException("payOS payment is disabled");
        }
        if (payOSProvider.getIfAvailable() == null) {
            throw new PaymentProviderException("payOS client is not configured");
        }
    }

    @Override
    public PayosCreatePaymentResult createPaymentLink(PayosCreatePaymentCommand command) {
        ensureAvailable();
        try {
            CreatePaymentLinkResponse response = payOSProvider.getObject()
                    .paymentRequests()
                    .create(buildRequest(command));
            return new PayosCreatePaymentResult(
                    response.getOrderCode(),
                    response.getPaymentLinkId(),
                    response.getCheckoutUrl(),
                    response.getQrCode(),
                    response.getStatus() == null ? null : response.getStatus().name(),
                    response.getExpiredAt()
            );
        } catch (RuntimeException ex) {
            throw new PaymentProviderException("Failed to create payOS payment link", ex);
        }
    }

    @Override
    public PayosWebhookVerificationResult verifyWebhook(Object webhook) {
        ensureAvailable();
        try {
            WebhookData data = payOSProvider.getObject().webhooks().verify(webhook);
            return new PayosWebhookVerificationResult(
                    data.getOrderCode(),
                    data.getAmount(),
                    data.getDescription(),
                    data.getReference(),
                    data.getTransactionDateTime(),
                    data.getCurrency(),
                    data.getPaymentLinkId(),
                    data.getCode(),
                    data.getDesc()
            );
        } catch (RuntimeException ex) {
            throw new BadRequestException("Invalid payOS webhook signature");
        }
    }

    @Override
    public PayosPaymentLinkResult getPaymentLink(String id) {
        ensureAvailable();
        try {
            PaymentLink link = payOSProvider.getObject().paymentRequests().get(id);
            return new PayosPaymentLinkResult(
                    link.getOrderCode(),
                    link.getAmount(),
                    link.getAmountPaid(),
                    link.getAmountRemaining(),
                    link.getId(),
                    link.getStatus() == null ? null : link.getStatus().name(),
                    link.getTransactions() == null
                            ? List.of()
                            : link.getTransactions().stream().map(this::toPaymentLinkTransaction).toList()
            );
        } catch (RuntimeException ex) {
            throw new PaymentProviderException("Failed to get payOS payment link", ex);
        }
    }

    @Override
    public void cancelPaymentLink(String id, String reason) {
        ensureAvailable();
        try {
            payOSProvider.getObject().paymentRequests().cancel(id, reason);
        } catch (RuntimeException ex) {
            throw new PaymentProviderException("Failed to cancel payOS payment link", ex);
        }
    }

    private CreatePaymentLinkRequest buildRequest(PayosCreatePaymentCommand command) {
        CreatePaymentLinkRequest.CreatePaymentLinkRequestBuilder builder = CreatePaymentLinkRequest.builder()
                .orderCode(command.orderCode())
                .amount(command.amountMinor())
                .description(command.description())
                .returnUrl(command.returnUrl())
                .cancelUrl(command.cancelUrl())
                .items(toPayosItems(command.items()));
        if (command.expiresAt() != null) {
            builder.expiredAt(command.expiresAt().atZone(ZoneId.systemDefault()).toEpochSecond());
        }
        return builder.build();
    }

    private List<PaymentLinkItem> toPayosItems(List<PayosCreatePaymentItem> items) {
        return items.stream()
                .map(this::toPayosItem)
                .toList();
    }

    private PaymentLinkItem toPayosItem(PayosCreatePaymentItem item) {
        return PaymentLinkItem.builder()
                .name(item.name())
                .quantity(item.quantity())
                .price(item.unitPriceMinor())
                .build();
    }

    private PayosPaymentLinkTransaction toPaymentLinkTransaction(Transaction transaction) {
        return new PayosPaymentLinkTransaction(
                transaction.getReference(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getTransactionDateTime()
        );
    }
}

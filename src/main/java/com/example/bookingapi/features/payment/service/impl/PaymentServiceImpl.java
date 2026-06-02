package com.example.bookingapi.features.payment.service.impl;

import com.example.bookingapi.common.config.PayosProperties;
import com.example.bookingapi.common.exception.BadRequestException;
import com.example.bookingapi.common.exception.ConflictException;
import com.example.bookingapi.common.exception.ResourceNotFoundException;
import com.example.bookingapi.common.security.UserPrincipal;
import com.example.bookingapi.features.booking.service.BookingPaymentItem;
import com.example.bookingapi.features.booking.service.BookingPaymentService;
import com.example.bookingapi.features.booking.service.BookingPaymentSnapshot;
import com.example.bookingapi.features.payment.dto.response.PaymentResponse;
import com.example.bookingapi.features.payment.exception.PaymentProviderException;
import com.example.bookingapi.features.payment.model.Payment;
import com.example.bookingapi.features.payment.model.enums.PaymentProvider;
import com.example.bookingapi.features.payment.model.enums.PaymentStatus;
import com.example.bookingapi.features.payment.repository.PaymentRepository;
import com.example.bookingapi.features.payment.service.PaymentCreationResult;
import com.example.bookingapi.features.payment.service.PaymentService;
import com.example.bookingapi.features.payment.service.PayosCreatePaymentCommand;
import com.example.bookingapi.features.payment.service.PayosCreatePaymentItem;
import com.example.bookingapi.features.payment.service.PayosCreatePaymentResult;
import com.example.bookingapi.features.payment.service.PayosPaymentClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private static final List<PaymentStatus> ACTIVE_PAYMENT_STATUSES = List.of(
            PaymentStatus.INITIATED,
            PaymentStatus.PENDING
    );
    private static final DateTimeFormatter ORDER_CODE_FORMATTER = DateTimeFormatter.ofPattern("yyMMddHHmmssS");

    private final PaymentRepository paymentRepository;
    private final BookingPaymentService bookingPaymentService;
    private final PayosPaymentClient payosPaymentClient;
    private final PayosProperties payosProperties;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            BookingPaymentService bookingPaymentService,
            PayosPaymentClient payosPaymentClient,
            PayosProperties payosProperties
    ) {
        this.paymentRepository = paymentRepository;
        this.bookingPaymentService = bookingPaymentService;
        this.payosPaymentClient = payosPaymentClient;
        this.payosProperties = payosProperties;
    }

    @Override
    @Transactional
    public PaymentCreationResult createPayosPayment(UUID bookingId, UserPrincipal currentUser) {
        payosPaymentClient.ensureAvailable();
        BookingPaymentSnapshot booking = bookingPaymentService.getPendingBookingForPayment(bookingId, currentUser);
        Long amountMinor = toVndMinor(booking.amount(), "Booking amount must be an integer VND amount");

        Payment activePayment = paymentRepository
                .findFirstByBooking_IdAndProviderAndStatusInOrderByAttemptNoDesc(
                        bookingId,
                        PaymentProvider.PAYOS,
                        ACTIVE_PAYMENT_STATUSES
                )
                .orElse(null);
        if (activePayment != null) {
            if (activePayment.getCheckoutUrl() != null && !activePayment.getCheckoutUrl().isBlank()) {
                return PaymentCreationResult.reused(toPaymentResponse(activePayment));
            }
            throw new ConflictException("payOS payment is already being created for this booking");
        }

        Payment payment = createInitiatedPayment(booking, amountMinor);
        bookingPaymentService.attachPaymentToActiveHolds(bookingId, payment);

        try {
            PayosCreatePaymentResult providerResult = payosPaymentClient.createPaymentLink(toPayosCommand(payment, booking));
            applyProviderResult(payment, providerResult);
            Payment savedPayment = paymentRepository.save(payment);
            return PaymentCreationResult.created(toPaymentResponse(savedPayment));
        } catch (PaymentProviderException ex) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(truncate(ex.getMessage(), 500));
            paymentRepository.save(payment);
            throw ex;
        }
    }

    @Override
    public PaymentResponse getPayment(UUID paymentId, UserPrincipal currentUser) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));
        if (!payment.getBooking().getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You don't have permission to view this payment");
        }
        return toPaymentResponse(payment);
    }

    private Payment createInitiatedPayment(BookingPaymentSnapshot booking, Long amountMinor) {
        Payment payment = new Payment();
        payment.setBooking(booking.booking());
        payment.setProvider(PaymentProvider.PAYOS);
        payment.setStatus(PaymentStatus.INITIATED);
        payment.setAmountMinor(amountMinor);
        payment.setCurrency(resolveCurrency(booking.currency()));
        payment.setAttemptNo(nextAttemptNo(booking.bookingId()));
        payment.setProviderOrderCode(generateOrderCode());
        payment.setReturnUrl(payosProperties.getReturnUrl());
        payment.setCancelUrl(payosProperties.getCancelUrl());
        payment.setExpiresAt(booking.expiresAt());
        return paymentRepository.saveAndFlush(payment);
    }

    private PayosCreatePaymentCommand toPayosCommand(Payment payment, BookingPaymentSnapshot booking) {
        Long orderCode = Long.valueOf(payment.getProviderOrderCode());
        return new PayosCreatePaymentCommand(
                orderCode,
                payment.getAmountMinor(),
                "Booking " + orderCode,
                payment.getReturnUrl(),
                payment.getCancelUrl(),
                payment.getExpiresAt(),
                booking.items().stream()
                        .map(this::toPayosItem)
                        .toList()
        );
    }

    private PayosCreatePaymentItem toPayosItem(BookingPaymentItem item) {
        return new PayosCreatePaymentItem(
                truncate(item.name(), 100),
                item.quantity(),
                toVndMinor(item.unitAmount(), "Booking line amount must be an integer VND amount")
        );
    }

    private void applyProviderResult(Payment payment, PayosCreatePaymentResult providerResult) {
        payment.setProviderOrderCode(providerResult.orderCode() == null
                ? payment.getProviderOrderCode()
                : String.valueOf(providerResult.orderCode()));
        payment.setProviderPaymentId(providerResult.paymentLinkId());
        payment.setCheckoutUrl(providerResult.checkoutUrl());
        payment.setQrCode(providerResult.qrCode());
        payment.setProviderStatus(providerResult.providerStatus());
        payment.setStatus(PaymentStatus.PENDING);
    }

    private Integer nextAttemptNo(UUID bookingId) {
        return paymentRepository.findFirstByBooking_IdAndProviderOrderByAttemptNoDesc(bookingId, PaymentProvider.PAYOS)
                .map(payment -> payment.getAttemptNo() + 1)
                .orElse(1);
    }

    private Long toVndMinor(BigDecimal amount, String errorMessage) {
        try {
            return amount.setScale(0, RoundingMode.UNNECESSARY).longValueExact();
        } catch (ArithmeticException ex) {
            throw new BadRequestException(errorMessage);
        }
    }

    private String resolveCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "VND";
        }
        if (!"VND".equalsIgnoreCase(currency)) {
            throw new BadRequestException("payOS only supports VND payments in this integration");
        }
        return "VND";
    }

    private String generateOrderCode() {
        String timestampPart = ORDER_CODE_FORMATTER.format(LocalDateTime.now());
        int randomPart = ThreadLocalRandom.current().nextInt(10, 100);
        return timestampPart + randomPart;
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getBooking().getId(),
                payment.getProvider().name(),
                payment.getStatus().name(),
                payment.getAmountMinor(),
                payment.getCurrency(),
                payment.getProviderOrderCode(),
                payment.getProviderPaymentId(),
                payment.getCheckoutUrl(),
                payment.getQrCode(),
                payment.getExpiresAt(),
                payment.getPaidAt(),
                payment.getCancelledAt()
        );
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}

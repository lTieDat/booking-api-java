package com.example.bookingapi.features.payment.service.impl;

import com.example.bookingapi.common.exception.BadRequestException;
import com.example.bookingapi.common.exception.ResourceNotFoundException;
import com.example.bookingapi.common.security.UserPrincipal;
import com.example.bookingapi.features.payment.dto.request.RefundRequest;
import com.example.bookingapi.features.payment.dto.response.RefundResponse;
import com.example.bookingapi.features.payment.model.Payment;
import com.example.bookingapi.features.payment.model.Refund;
import com.example.bookingapi.features.payment.model.enums.PaymentStatus;
import com.example.bookingapi.features.payment.model.enums.RefundStatus;
import com.example.bookingapi.features.payment.repository.PaymentRepository;
import com.example.bookingapi.features.payment.repository.RefundRepository;
import com.example.bookingapi.features.payment.service.RefundService;
import com.example.bookingapi.features.booking.service.BookingPaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RefundServiceImpl implements RefundService {

    private static final List<RefundStatus> EFFECTIVE_REFUND_STATUSES = List.of(
            RefundStatus.REQUESTED,
            RefundStatus.PROCESSING,
            RefundStatus.SUCCEEDED
    );

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final BookingPaymentService bookingPaymentService;

    public RefundServiceImpl(
            PaymentRepository paymentRepository,
            RefundRepository refundRepository,
            BookingPaymentService bookingPaymentService
    ) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.bookingPaymentService = bookingPaymentService;
    }

    @Override
    @Transactional
    public RefundResponse requestManualRefund(UUID paymentId, RefundRequest request, UserPrincipal currentUser) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));
        if (payment.getStatus() != PaymentStatus.PAID && payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new BadRequestException("Only paid payments can be refunded");
        }

        long requestedAmount = request.getAmountMinor() == null ? payment.getAmountMinor() : request.getAmountMinor();
        long alreadyRefunded = refundRepository
                .findByPayment_IdAndStatusIn(paymentId, EFFECTIVE_REFUND_STATUSES)
                .stream()
                .mapToLong(Refund::getAmountMinor)
                .sum();
        if (alreadyRefunded + requestedAmount > payment.getAmountMinor()) {
            throw new BadRequestException("Refund amount exceeds paid amount");
        }

        Refund refund = new Refund();
        refund.setPayment(payment);
        refund.setAmountMinor(requestedAmount);
        refund.setCurrency(payment.getCurrency());
        refund.setStatus(RefundStatus.SUCCEEDED);
        refund.setReason(request.getReason().trim());
        refund.setRequestedBy(currentUser.getId());
        refund.setRequestedAt(LocalDateTime.now());
        refund.setProcessedAt(LocalDateTime.now());
        Refund savedRefund = refundRepository.save(refund);

        long totalRefunded = alreadyRefunded + requestedAmount;
        if (totalRefunded == payment.getAmountMinor()) {
            payment.setStatus(PaymentStatus.REFUNDED);
            bookingPaymentService.markRefundedBooking(
                    payment.getBooking().getId(),
                    savedRefund.getId(),
                    "Booking refunded manually: " + savedRefund.getReason()
            );
        } else {
            payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
        }
        paymentRepository.save(payment);
        return toRefundResponse(savedRefund);
    }

    private RefundResponse toRefundResponse(Refund refund) {
        return new RefundResponse(
                refund.getId(),
                refund.getPayment().getId(),
                refund.getPayment().getBooking().getId(),
                refund.getAmountMinor(),
                refund.getCurrency(),
                refund.getStatus().name(),
                refund.getReason(),
                refund.getRequestedAt(),
                refund.getProcessedAt()
        );
    }
}

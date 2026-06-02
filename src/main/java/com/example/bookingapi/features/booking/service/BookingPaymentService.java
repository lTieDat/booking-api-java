package com.example.bookingapi.features.booking.service;

import com.example.bookingapi.common.security.UserPrincipal;
import com.example.bookingapi.features.booking.model.enums.BookingStatus;
import com.example.bookingapi.features.payment.model.Payment;

import java.util.UUID;

public interface BookingPaymentService {
    BookingPaymentSnapshot getPendingBookingForPayment(UUID bookingId, UserPrincipal currentUser);
    void attachPaymentToActiveHolds(UUID bookingId, Payment payment);
    void confirmPaidBooking(UUID bookingId, UUID paymentId);
    void releaseUnpaidBooking(UUID bookingId, UUID paymentId, BookingStatus terminalStatus, String reason);
    void markRefundedBooking(UUID bookingId, UUID refundId, String reason);
}

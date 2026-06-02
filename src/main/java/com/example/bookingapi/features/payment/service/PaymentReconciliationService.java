package com.example.bookingapi.features.payment.service;

public interface PaymentReconciliationService {
    int reconcileExpiredOrPendingPayments();
}

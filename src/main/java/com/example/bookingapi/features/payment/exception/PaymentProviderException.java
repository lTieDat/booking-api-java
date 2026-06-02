package com.example.bookingapi.features.payment.exception;

import com.example.bookingapi.common.exception.AppException;

public class PaymentProviderException extends AppException {
    public PaymentProviderException(String message) {
        super(message);
    }

    public PaymentProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}

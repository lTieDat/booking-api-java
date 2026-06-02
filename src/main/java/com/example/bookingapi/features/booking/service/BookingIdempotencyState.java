package com.example.bookingapi.features.booking.service;

import java.util.UUID;

public record BookingIdempotencyState(
        Status status,
        String requestHash,
        UUID bookingId
) {

    private static final String SEPARATOR = "|";

    public static BookingIdempotencyState processing(String requestHash) {
        return new BookingIdempotencyState(Status.PROCESSING, requestHash, null);
    }

    public static BookingIdempotencyState completed(String requestHash, UUID bookingId) {
        return new BookingIdempotencyState(Status.COMPLETED, requestHash, bookingId);
    }

    public boolean isProcessing() {
        return status == Status.PROCESSING;
    }

    public boolean isCompleted() {
        return status == Status.COMPLETED;
    }

    public String serialize() {
        String bookingIdValue = bookingId == null ? "" : bookingId.toString();
        return status.name() + SEPARATOR + requestHash + SEPARATOR + bookingIdValue;
    }

    public static BookingIdempotencyState deserialize(String value) {
        String[] parts = value.split("\\|", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid booking idempotency state");
        }
        UUID parsedBookingId = parts[2].isBlank() ? null : UUID.fromString(parts[2]);
        return new BookingIdempotencyState(Status.valueOf(parts[0]), parts[1], parsedBookingId);
    }

    public enum Status {
        PROCESSING,
        COMPLETED
    }
}

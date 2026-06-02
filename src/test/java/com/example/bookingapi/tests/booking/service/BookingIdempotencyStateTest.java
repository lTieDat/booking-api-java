package com.example.bookingapi.tests.booking.service;

import com.example.bookingapi.features.booking.service.BookingIdempotencyState;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BookingIdempotencyStateTest {

    @Test
    void serializesAndDeserializesProcessingState() {
        BookingIdempotencyState state = BookingIdempotencyState.processing("request-hash");

        BookingIdempotencyState parsed = BookingIdempotencyState.deserialize(state.serialize());

        assertThat(parsed.isProcessing()).isTrue();
        assertThat(parsed.requestHash()).isEqualTo("request-hash");
        assertThat(parsed.bookingId()).isNull();
    }

    @Test
    void serializesAndDeserializesCompletedState() {
        UUID bookingId = UUID.randomUUID();
        BookingIdempotencyState state = BookingIdempotencyState.completed("request-hash", bookingId);

        BookingIdempotencyState parsed = BookingIdempotencyState.deserialize(state.serialize());

        assertThat(parsed.isCompleted()).isTrue();
        assertThat(parsed.requestHash()).isEqualTo("request-hash");
        assertThat(parsed.bookingId()).isEqualTo(bookingId);
    }
}

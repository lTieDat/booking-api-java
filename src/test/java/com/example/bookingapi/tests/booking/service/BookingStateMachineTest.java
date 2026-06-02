package com.example.bookingapi.tests.booking.service;

import com.example.bookingapi.common.exception.BadRequestException;
import com.example.bookingapi.features.booking.model.Booking;
import com.example.bookingapi.features.booking.model.enums.BookingStatus;
import com.example.bookingapi.features.booking.service.BookingStateMachine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingStateMachineTest {

    private final BookingStateMachine stateMachine = new BookingStateMachine();

    @Test
    void initialStatusIsPending() {
        assertEquals(BookingStatus.PENDING, stateMachine.initialStatus());
    }

    @Test
    void allowsExpectedBookingLifecycleTransitions() {
        assertTrue(stateMachine.canTransition(BookingStatus.PENDING, BookingStatus.CONFIRMED));
        assertTrue(stateMachine.canTransition(BookingStatus.PENDING, BookingStatus.CANCELLED));
        assertTrue(stateMachine.canTransition(BookingStatus.PENDING, BookingStatus.EXPIRED));
        assertTrue(stateMachine.canTransition(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN));
        assertTrue(stateMachine.canTransition(BookingStatus.CONFIRMED, BookingStatus.CANCELLED));
        assertTrue(stateMachine.canTransition(BookingStatus.CONFIRMED, BookingStatus.NO_SHOW));
        assertTrue(stateMachine.canTransition(BookingStatus.CHECKED_IN, BookingStatus.CHECKED_OUT));
        assertTrue(stateMachine.canTransition(BookingStatus.CHECKED_OUT, BookingStatus.REFUNDED));
        assertTrue(stateMachine.canTransition(BookingStatus.CANCELLED, BookingStatus.REFUNDED));
    }

    @Test
    void rejectsInvalidTransitions() {
        assertFalse(stateMachine.canTransition(BookingStatus.PENDING, BookingStatus.CHECKED_IN));
        assertFalse(stateMachine.canTransition(BookingStatus.CHECKED_OUT, BookingStatus.CHECKED_IN));
        assertFalse(stateMachine.canTransition(BookingStatus.REFUNDED, BookingStatus.CANCELLED));
        assertFalse(stateMachine.canTransition(BookingStatus.NO_SHOW, BookingStatus.CONFIRMED));
        assertFalse(stateMachine.canTransition(BookingStatus.EXPIRED, BookingStatus.CONFIRMED));
    }

    @Test
    void transitionUpdatesBookingStatusWhenAllowed() {
        Booking booking = new Booking();
        booking.setStatus(BookingStatus.PENDING);

        stateMachine.transition(booking, BookingStatus.CONFIRMED);

        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
    }

    @Test
    void transitionThrowsWhenNotAllowed() {
        Booking booking = new Booking();
        booking.setStatus(BookingStatus.PENDING);

        assertThrows(BadRequestException.class, () -> stateMachine.transition(booking, BookingStatus.CHECKED_IN));
    }
}

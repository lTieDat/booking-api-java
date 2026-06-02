package com.example.bookingapi.features.booking.service;

import com.example.bookingapi.common.exception.BadRequestException;
import com.example.bookingapi.features.booking.model.Booking;
import com.example.bookingapi.features.booking.model.enums.BookingStatus;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class BookingStateMachine {

    private static final Map<BookingStatus, Set<BookingStatus>> ALLOWED_TRANSITIONS = buildAllowedTransitions();

    public BookingStatus initialStatus() {
        return BookingStatus.PENDING;
    }

    public void transition(Booking booking, BookingStatus targetStatus) {
        BookingStatus currentStatus = booking.getStatus();
        assertCanTransition(currentStatus, targetStatus);
        booking.setStatus(targetStatus);
    }

    public void assertCanTransition(BookingStatus currentStatus, BookingStatus targetStatus) {
        if (currentStatus == null) {
            throw new BadRequestException("Booking status is missing");
        }
        if (targetStatus == null) {
            throw new BadRequestException("Target booking status is required");
        }
        if (!canTransition(currentStatus, targetStatus)) {
            throw new BadRequestException(
                    "Invalid booking status transition: " + currentStatus + " -> " + targetStatus
            );
        }
    }

    public boolean canTransition(BookingStatus currentStatus, BookingStatus targetStatus) {
        return ALLOWED_TRANSITIONS
                .getOrDefault(currentStatus, Set.of())
                .contains(targetStatus);
    }

    public Set<BookingStatus> getAllowedTransitions(BookingStatus currentStatus) {
        return ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());
    }

    private static Map<BookingStatus, Set<BookingStatus>> buildAllowedTransitions() {
        EnumMap<BookingStatus, Set<BookingStatus>> transitions = new EnumMap<>(BookingStatus.class);
        transitions.put(BookingStatus.PENDING, immutableEnumSet(
                BookingStatus.CONFIRMED,
                BookingStatus.CANCELLED,
                BookingStatus.EXPIRED
        ));
        transitions.put(BookingStatus.CONFIRMED, immutableEnumSet(
                BookingStatus.CHECKED_IN,
                BookingStatus.CANCELLED,
                BookingStatus.NO_SHOW
        ));
        transitions.put(BookingStatus.CHECKED_IN, immutableEnumSet(BookingStatus.CHECKED_OUT));
        transitions.put(BookingStatus.CHECKED_OUT, immutableEnumSet(BookingStatus.REFUNDED));
        transitions.put(BookingStatus.CANCELLED, immutableEnumSet(BookingStatus.REFUNDED));
        transitions.put(BookingStatus.REFUNDED, Set.of());
        transitions.put(BookingStatus.NO_SHOW, Set.of());
        transitions.put(BookingStatus.EXPIRED, Set.of());
        return Collections.unmodifiableMap(transitions);
    }

    private static Set<BookingStatus> immutableEnumSet(BookingStatus first, BookingStatus... rest) {
        EnumSet<BookingStatus> statuses = EnumSet.of(first, rest);
        return Collections.unmodifiableSet(statuses);
    }
}

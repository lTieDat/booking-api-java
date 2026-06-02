package com.example.bookingapi.tests.booking.controller;

import com.example.bookingapi.features.booking.controller.BookingController;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BookingControllerSecurityAnnotationsTest {

    @Test
    void frontDeskBookingActionsAllowAdminAndReceptionist() throws NoSuchMethodException {
        assertPreAuthorizeValue("checkInBooking");
        assertPreAuthorizeValue("checkOutBooking");
        assertPreAuthorizeValue("markNoShow");
    }

    private void assertPreAuthorizeValue(String methodName) throws NoSuchMethodException {
        Method method = BookingController.class.getMethod(methodName, java.util.UUID.class,
                com.example.bookingapi.common.security.UserPrincipal.class);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation);
        assertEquals("hasAnyRole('ADMIN', 'RECEPTIONIST')", annotation.value());
    }
}

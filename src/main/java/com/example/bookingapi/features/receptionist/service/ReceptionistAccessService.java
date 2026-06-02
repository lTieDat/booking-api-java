package com.example.bookingapi.features.receptionist.service;

import com.example.bookingapi.common.security.UserPrincipal;
import com.example.bookingapi.features.booking.model.Booking;

public interface ReceptionistAccessService {
    void requireCanManageBooking(Booking booking, UserPrincipal currentUser);
}

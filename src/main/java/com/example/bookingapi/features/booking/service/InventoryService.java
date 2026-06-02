package com.example.bookingapi.features.booking.service;

import com.example.bookingapi.features.booking.model.BookedRoom;
import com.example.bookingapi.features.booking.model.Booking;
import com.example.bookingapi.features.booking.model.enums.InventoryHoldStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface InventoryService {
    void holdInventory(Booking booking, List<BookedRoom> bookedRooms, LocalDate checkIn, LocalDate checkOut, LocalDateTime expiresAt);
    void consumeActiveHolds(Booking booking);
    void releaseActiveHolds(Booking booking, InventoryHoldStatus releasedStatus);
    void releaseConsumedHolds(Booking booking);
}

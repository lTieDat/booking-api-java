package com.example.bookingapi.features.booking.service.impl;

import com.example.bookingapi.common.exception.BadRequestException;
import com.example.bookingapi.features.booking.model.BookedRoom;
import com.example.bookingapi.features.booking.model.Booking;
import com.example.bookingapi.features.booking.model.InventoryHold;
import com.example.bookingapi.features.booking.model.RoomInventory;
import com.example.bookingapi.features.booking.model.enums.BookingStatus;
import com.example.bookingapi.features.booking.model.enums.InventoryHoldStatus;
import com.example.bookingapi.features.booking.repository.BookingRepository;
import com.example.bookingapi.features.booking.repository.InventoryHoldRepository;
import com.example.bookingapi.features.booking.repository.RoomInventoryRepository;
import com.example.bookingapi.features.booking.service.InventoryService;
import com.example.bookingapi.features.room.model.RoomType;
import com.example.bookingapi.features.room.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class InventoryServiceImpl implements InventoryService {

    @Autowired private RoomInventoryRepository roomInventoryRepository;
    @Autowired private InventoryHoldRepository inventoryHoldRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private RoomRepository roomRepository;

    @Override
    public void holdInventory(
            Booking booking,
            List<BookedRoom> bookedRooms,
            LocalDate checkIn,
            LocalDate checkOut,
            LocalDateTime expiresAt
    ) {
        Map<UUID, RoomTypeQuantity> quantities = mergeQuantitiesByRoomType(bookedRooms);
        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            for (RoomTypeQuantity item : quantities.values()) {
                RoomInventory inventory = getOrCreateInventory(item.roomType(), date);
                if (inventory.getAvailableUnits() < item.quantity()) {
                    throw new BadRequestException(
                            "Not enough inventory for room type " + item.roomType().getId() + " on " + date
                    );
                }

                inventory.hold(item.quantity());
                roomInventoryRepository.save(inventory);
                inventoryHoldRepository.save(buildHold(booking, item.roomType(), date, item.quantity(), expiresAt));
            }
        }
    }

    @Override
    public void consumeActiveHolds(Booking booking) {
        List<InventoryHold> holds = inventoryHoldRepository.findByBooking_IdAndStatus(
                booking.getId(), InventoryHoldStatus.ACTIVE);
        for (InventoryHold hold : holds) {
            RoomInventory inventory = getLockedInventory(hold);
            inventory.consumeHold(hold.getQuantity());
            hold.setStatus(InventoryHoldStatus.CONSUMED);
            roomInventoryRepository.save(inventory);
            inventoryHoldRepository.save(hold);
        }
    }

    @Override
    public void releaseActiveHolds(Booking booking, InventoryHoldStatus releasedStatus) {
        List<InventoryHold> holds = inventoryHoldRepository.findByBooking_IdAndStatus(
                booking.getId(), InventoryHoldStatus.ACTIVE);
        LocalDateTime releasedAt = LocalDateTime.now();
        for (InventoryHold hold : holds) {
            RoomInventory inventory = getLockedInventory(hold);
            inventory.releaseHold(hold.getQuantity());
            hold.setStatus(releasedStatus);
            hold.setReleasedAt(releasedAt);
            roomInventoryRepository.save(inventory);
            inventoryHoldRepository.save(hold);
        }
    }

    @Override
    public void releaseConsumedHolds(Booking booking) {
        List<InventoryHold> holds = inventoryHoldRepository.findByBooking_IdAndStatus(
                booking.getId(), InventoryHoldStatus.CONSUMED);
        if (holds.isEmpty()) {
            releaseBookedWithoutHolds(booking);
            return;
        }

        LocalDateTime releasedAt = LocalDateTime.now();
        for (InventoryHold hold : holds) {
            RoomInventory inventory = getLockedInventory(hold);
            inventory.releaseBooked(hold.getQuantity());
            hold.setStatus(InventoryHoldStatus.RELEASED);
            hold.setReleasedAt(releasedAt);
            roomInventoryRepository.save(inventory);
            inventoryHoldRepository.save(hold);
        }
    }

    private void releaseBookedWithoutHolds(Booking booking) {
        Map<UUID, RoomTypeQuantity> quantities = mergeQuantitiesByRoomType(booking.getBookedRooms());
        LocalDate checkIn = booking.getCheckInDateTime().toLocalDate();
        LocalDate checkOut = booking.getCheckOutDateTime().toLocalDate();
        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            for (RoomTypeQuantity item : quantities.values()) {
                roomInventoryRepository.findByRoomType_IdAndDate(item.roomType().getId(), date)
                        .ifPresent(inventory -> {
                            inventory.releaseBooked(item.quantity());
                            roomInventoryRepository.save(inventory);
                        });
            }
        }
    }

    private Map<UUID, RoomTypeQuantity> mergeQuantitiesByRoomType(List<BookedRoom> bookedRooms) {
        Map<UUID, RoomTypeQuantity> quantities = new LinkedHashMap<>();
        for (BookedRoom bookedRoom : bookedRooms) {
            UUID roomTypeId = bookedRoom.getRoomType().getId();
            quantities.compute(roomTypeId, (id, existing) -> {
                if (existing == null) {
                    return new RoomTypeQuantity(bookedRoom.getRoomType(), bookedRoom.getQuantity());
                }
                return new RoomTypeQuantity(existing.roomType(), existing.quantity() + bookedRoom.getQuantity());
            });
        }
        return quantities;
    }

    private InventoryHold buildHold(
            Booking booking,
            RoomType roomType,
            LocalDate date,
            int quantity,
            LocalDateTime expiresAt
    ) {
        InventoryHold hold = new InventoryHold();
        hold.setBooking(booking);
        hold.setRoomType(roomType);
        hold.setDate(date);
        hold.setQuantity(quantity);
        hold.setStatus(InventoryHoldStatus.ACTIVE);
        hold.setExpiresAt(expiresAt);
        return hold;
    }

    private RoomInventory getOrCreateInventory(RoomType roomType, LocalDate date) {
        return roomInventoryRepository.findByRoomType_IdAndDate(roomType.getId(), date)
                .orElseGet(() -> createInventory(roomType, date));
    }

    private RoomInventory createInventory(RoomType roomType, LocalDate date) {
        long totalUnits = roomRepository.countByRoomType_IdAndIsActiveTrue(roomType.getId());
        long bookedUnits = bookingRepository.sumBookedQuantityForRoomTypeAndDate(
                roomType.getId(),
                date,
                BookingStatus.CONFIRMED.name(),
                BookingStatus.CHECKED_IN.name()
        );
        if (bookedUnits > totalUnits) {
            throw new BadRequestException(
                    "Existing bookings exceed inventory for room type " + roomType.getId() + " on " + date
            );
        }

        RoomInventory inventory = new RoomInventory();
        inventory.setRoomType(roomType);
        inventory.setDate(date);
        inventory.setTotalUnits(Math.toIntExact(totalUnits));
        inventory.setBookedUnits(Math.toIntExact(bookedUnits));
        inventory.setHeldUnits(0);
        inventory.setAvailableUnits(Math.toIntExact(totalUnits - bookedUnits));
        try {
            roomInventoryRepository.saveAndFlush(inventory);
        } catch (DataIntegrityViolationException ex) {
            return roomInventoryRepository.findByRoomType_IdAndDate(roomType.getId(), date)
                    .orElseThrow(() -> ex);
        }
        return roomInventoryRepository.findByRoomType_IdAndDate(roomType.getId(), date)
                .orElse(inventory);
    }

    private RoomInventory getLockedInventory(InventoryHold hold) {
        return roomInventoryRepository.findByRoomType_IdAndDate(hold.getRoomType().getId(), hold.getDate())
                .orElseThrow(() -> new BadRequestException("Inventory row is missing for hold " + hold.getId()));
    }

    private record RoomTypeQuantity(RoomType roomType, int quantity) {
    }
}

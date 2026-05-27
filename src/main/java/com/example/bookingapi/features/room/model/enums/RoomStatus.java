package com.example.bookingapi.features.room.model.enums;

public enum RoomStatus {
    AVAILABLE,
    OCCUPIED,
    MAINTENANCE,
    CLEANING,
    OUT_OF_SERVICE;

    public static RoomStatus fromString(String status) {
        for (RoomStatus roomStatus : RoomStatus.values()) {
            if (roomStatus.name().equalsIgnoreCase(status)) {
                return roomStatus;
            }
        }
        throw new IllegalArgumentException("Unknown room status: " + status);
    }
}

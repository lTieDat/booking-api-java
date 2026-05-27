package com.example.bookingapi.features.room.model.enums;

public enum BedType {
    DOUBLE,
    SINGLE;

    public static BedType isValidType (String type){
        for (BedType bedType : BedType.values()) {
            if (bedType.name().equalsIgnoreCase(type)) {
                return bedType;
            }
        }
        throw new IllegalArgumentException("Unknown bed type: " + type);
    }
}


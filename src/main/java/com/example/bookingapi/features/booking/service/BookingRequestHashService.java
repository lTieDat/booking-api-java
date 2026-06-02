package com.example.bookingapi.features.booking.service;

import com.example.bookingapi.features.booking.dto.request.BookedRoomRequest;
import com.example.bookingapi.features.booking.dto.request.BookingGuestRequest;
import com.example.bookingapi.features.booking.dto.request.BookingRequest;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class BookingRequestHashService {

    public String hash(BookingRequest request) {
        try {
            String canonicalRequest = toCanonicalRequest(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonicalRequest.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private String toCanonicalRequest(BookingRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append("checkIn=").append(request.getCheckInDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        builder.append("|checkOut=").append(request.getCheckOutDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        builder.append("|guest=").append(toCanonicalGuest(request.getGuest()));
        builder.append("|rooms=");
        for (CanonicalRoom room : toCanonicalRooms(request.getRooms())) {
            builder.append(room.roomTypeId()).append(':').append(room.quantity()).append(';');
        }
        return builder.toString();
    }

    private String toCanonicalGuest(BookingGuestRequest guest) {
        return String.join("|",
                normalize(guest.getFirstName()),
                normalize(guest.getLastName()),
                normalizeNullable(guest.getMiddleName()),
                normalize(guest.getIdentifyCardNo()),
                normalize(guest.getPhoneNumber()),
                normalize(guest.getEmail()).toLowerCase()
        );
    }

    private List<CanonicalRoom> toCanonicalRooms(List<BookedRoomRequest> rooms) {
        Map<String, Integer> quantitiesByRoomType = new TreeMap<>();
        for (BookedRoomRequest room : rooms) {
            quantitiesByRoomType.merge(room.getRoomTypeId().toString(), room.getQuantity(), Integer::sum);
        }
        return quantitiesByRoomType.entrySet().stream()
                .map(entry -> new CanonicalRoom(entry.getKey(), entry.getValue()))
                .toList();
    }

    private String normalize(String value) {
        return value.trim();
    }

    private String normalizeNullable(String value) {
        return value == null ? null : value.trim();
    }

    private record CanonicalRoom(String roomTypeId, Integer quantity) {
    }
}

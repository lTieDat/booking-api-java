package com.example.bookingapi.tests.booking.service;

import com.example.bookingapi.features.booking.dto.request.BookedRoomRequest;
import com.example.bookingapi.features.booking.dto.request.BookingGuestRequest;
import com.example.bookingapi.features.booking.dto.request.BookingRequest;
import com.example.bookingapi.features.booking.service.BookingRequestHashService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BookingRequestHashServiceTest {

    private final BookingRequestHashService hashService = new BookingRequestHashService();

    @Test
    void hashReturnsSameValueForEquivalentCanonicalRequest() {
        UUID standardRoomTypeId = UUID.fromString("41000000-0000-0000-0000-000000000001");
        UUID deluxeRoomTypeId = UUID.fromString("41000000-0000-0000-0000-000000000002");

        BookingRequest first = request(List.of(
                room(standardRoomTypeId, 1),
                room(deluxeRoomTypeId, 2)
        ));
        BookingRequest retry = request(List.of(
                room(deluxeRoomTypeId, 1),
                room(standardRoomTypeId, 1),
                room(deluxeRoomTypeId, 1)
        ));
        retry.getGuest().setFirstName(" Alice ");
        retry.getGuest().setEmail("ALICE@EXAMPLE.COM");

        assertThat(hashService.hash(retry)).isEqualTo(hashService.hash(first));
    }

    @Test
    void hashReturnsDifferentValueWhenBusinessDataChanges() {
        UUID roomTypeId = UUID.fromString("41000000-0000-0000-0000-000000000001");

        BookingRequest first = request(List.of(room(roomTypeId, 1)));
        BookingRequest changed = request(List.of(room(roomTypeId, 2)));

        assertThat(hashService.hash(changed)).isNotEqualTo(hashService.hash(first));
    }

    private BookingRequest request(List<BookedRoomRequest> rooms) {
        BookingRequest request = new BookingRequest();
        request.setCheckInDate(LocalDateTime.of(2026, 6, 10, 14, 0));
        request.setCheckOutDate(LocalDateTime.of(2026, 6, 12, 12, 0));
        request.setGuest(guest());
        request.setRooms(rooms);
        return request;
    }

    private BookingGuestRequest guest() {
        BookingGuestRequest guest = new BookingGuestRequest();
        guest.setFirstName("Alice");
        guest.setLastName("Nguyen");
        guest.setMiddleName("Thi");
        guest.setIdentifyCardNo("ID123456");
        guest.setPhoneNumber("0900000000");
        guest.setEmail("alice@example.com");
        return guest;
    }

    private BookedRoomRequest room(UUID roomTypeId, int quantity) {
        BookedRoomRequest room = new BookedRoomRequest();
        room.setRoomTypeId(roomTypeId);
        room.setQuantity(quantity);
        return room;
    }
}

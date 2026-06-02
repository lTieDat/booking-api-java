package com.example.bookingapi.tests.receptionist.service;

import com.example.bookingapi.common.security.UserPrincipal;
import com.example.bookingapi.features.auth.model.enums.ActorType;
import com.example.bookingapi.features.booking.model.BookedRoom;
import com.example.bookingapi.features.booking.model.Booking;
import com.example.bookingapi.features.hotel.model.Hotel;
import com.example.bookingapi.features.receptionist.repository.ReceptionistAssignmentRepository;
import com.example.bookingapi.features.receptionist.service.impl.ReceptionistAccessServiceImpl;
import com.example.bookingapi.features.room.model.RoomType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReceptionistAccessServiceImplTest {

    @Mock
    private ReceptionistAssignmentRepository receptionistAssignmentRepository;

    @Test
    void adminCanManageAnyBookingWithoutAssignmentLookup() {
        ReceptionistAccessServiceImpl service = new ReceptionistAccessServiceImpl(receptionistAssignmentRepository);
        Booking booking = bookingInHotel(UUID.randomUUID());

        assertDoesNotThrow(() -> service.requireCanManageBooking(booking, principal("ROLE_ADMIN")));

        verifyNoInteractions(receptionistAssignmentRepository);
    }

    @Test
    void assignedReceptionistCanManageBookingInAssignedHotel() {
        ReceptionistAccessServiceImpl service = new ReceptionistAccessServiceImpl(receptionistAssignmentRepository);
        UUID userId = UUID.randomUUID();
        UUID hotelId = UUID.randomUUID();
        Booking booking = bookingInHotel(hotelId);

        when(receptionistAssignmentRepository.existsByUser_IdAndHotel_IdAndIsActiveTrue(userId, hotelId))
                .thenReturn(true);

        assertDoesNotThrow(() -> service.requireCanManageBooking(booking, principal(userId, "ROLE_RECEPTIONIST")));

        verify(receptionistAssignmentRepository).existsByUser_IdAndHotel_IdAndIsActiveTrue(userId, hotelId);
    }

    @Test
    void unassignedReceptionistCannotManageBookingInOtherHotel() {
        ReceptionistAccessServiceImpl service = new ReceptionistAccessServiceImpl(receptionistAssignmentRepository);
        UUID userId = UUID.randomUUID();
        UUID hotelId = UUID.randomUUID();
        Booking booking = bookingInHotel(hotelId);

        when(receptionistAssignmentRepository.existsByUser_IdAndHotel_IdAndIsActiveTrue(userId, hotelId))
                .thenReturn(false);

        assertThrows(
                AccessDeniedException.class,
                () -> service.requireCanManageBooking(booking, principal(userId, "ROLE_RECEPTIONIST"))
        );

        verify(receptionistAssignmentRepository).existsByUser_IdAndHotel_IdAndIsActiveTrue(userId, hotelId);
    }

    @Test
    void regularUserCannotManageBookingWithoutAssignmentLookup() {
        ReceptionistAccessServiceImpl service = new ReceptionistAccessServiceImpl(receptionistAssignmentRepository);
        Booking booking = bookingInHotel(UUID.randomUUID());

        assertThrows(
                AccessDeniedException.class,
                () -> service.requireCanManageBooking(booking, principal("ROLE_USER"))
        );

        verify(receptionistAssignmentRepository, never())
                .existsByUser_IdAndHotel_IdAndIsActiveTrue(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private Booking bookingInHotel(UUID hotelId) {
        Hotel hotel = new Hotel();
        hotel.setId(hotelId);

        RoomType roomType = new RoomType();
        roomType.setId(UUID.randomUUID());
        roomType.setHotel(hotel);

        BookedRoom bookedRoom = new BookedRoom();
        bookedRoom.setRoomType(roomType);

        Booking booking = new Booking();
        booking.addBookedRoom(bookedRoom);
        return booking;
    }

    private UserPrincipal principal(String role) {
        return principal(UUID.randomUUID(), role);
    }

    private UserPrincipal principal(UUID userId, String role) {
        return new UserPrincipal(
                userId,
                "Front Desk",
                "reception",
                "reception@booking.local",
                "password",
                List.of(new SimpleGrantedAuthority(role)),
                ActorType.USER,
                true
        );
    }
}

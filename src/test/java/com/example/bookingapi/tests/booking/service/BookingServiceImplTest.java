package com.example.bookingapi.tests.booking.service;

import com.example.bookingapi.common.security.UserPrincipal;
import com.example.bookingapi.features.auth.model.enums.ActorType;
import com.example.bookingapi.features.booking.dto.request.BookedRoomRequest;
import com.example.bookingapi.features.booking.dto.request.BookingGuestRequest;
import com.example.bookingapi.features.booking.dto.request.BookingRequest;
import com.example.bookingapi.features.booking.model.Booking;
import com.example.bookingapi.features.booking.model.BookingStatusLog;
import com.example.bookingapi.features.booking.model.Guest;
import com.example.bookingapi.features.booking.model.enums.BookingStatus;
import com.example.bookingapi.features.booking.repository.BookingRepository;
import com.example.bookingapi.features.booking.repository.BookingStatusLogRepository;
import com.example.bookingapi.features.booking.repository.GuestRepository;
import com.example.bookingapi.features.booking.service.BookingIdempotencyCacheService;
import com.example.bookingapi.features.booking.service.BookingRequestHashService;
import com.example.bookingapi.features.booking.service.InventoryService;
import com.example.bookingapi.features.booking.service.BookingStateMachine;
import com.example.bookingapi.features.booking.service.impl.BookingServiceImpl;
import com.example.bookingapi.features.room.model.RoomType;
import com.example.bookingapi.features.room.repository.RoomTypeRepository;
import com.example.bookingapi.features.user.model.User;
import com.example.bookingapi.features.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private BookingStatusLogRepository bookingStatusLogRepository;
    @Mock private RoomTypeRepository roomTypeRepository;
    @Mock private UserRepository userRepository;
    @Mock private BookingStateMachine bookingStateMachine;
    @Mock private GuestRepository guestRepository;
    @Mock private InventoryService inventoryService;
    @Mock private BookingRequestHashService bookingRequestHashService;
    @Mock private BookingIdempotencyCacheService bookingIdempotencyCacheService;
    @Mock private TransactionTemplate transactionTemplate;

    @InjectMocks private BookingServiceImpl bookingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(bookingService, "pendingExpirationMinutes", 15L);
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    @Test
    void createBookingSetsPendingExpiration() {
        UUID userId = UUID.randomUUID();
        UUID roomTypeId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        RoomType roomType = new RoomType();
        roomType.setId(roomTypeId);
        roomType.setName("Deluxe");
        roomType.setCode("DLX");
        roomType.setMaxOccupancy(2);
        roomType.setBasePrice(BigDecimal.valueOf(1_000_000));

        BookingRequest request = new BookingRequest();
        request.setCheckInDate(LocalDateTime.now().plusDays(1));
        request.setCheckOutDate(LocalDateTime.now().plusDays(2));
        BookedRoomRequest bookedRoomRequest = new BookedRoomRequest();
        bookedRoomRequest.setRoomTypeId(roomTypeId);
        bookedRoomRequest.setQuantity(1);
        request.setRooms(List.of(bookedRoomRequest));
        request.setGuest(buildGuestRequest());

        UserPrincipal currentUser = new UserPrincipal(
                userId,
                "User",
                "user",
                "user@example.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                ActorType.USER,
                true
        );
        LocalDateTime beforeCreate = LocalDateTime.now();

        when(bookingStateMachine.initialStatus()).thenReturn(BookingStatus.PENDING);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roomTypeRepository.findById(roomTypeId)).thenReturn(Optional.of(roomType));
        when(guestRepository.findByIdentifyCardNo("ID123456")).thenReturn(Optional.empty());
        when(guestRepository.findByEmail("guest@example.com")).thenReturn(Optional.empty());
        when(guestRepository.save(any(Guest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingRequestHashService.hash(request)).thenReturn("request-hash");
        when(bookingIdempotencyCacheService.find(userId, "create-booking-1")).thenReturn(Optional.empty());
        when(bookingRepository.findByUser_IdAndClientRequestId(userId, "create-booking-1")).thenReturn(Optional.empty());
        when(bookingIdempotencyCacheService.putProcessingIfAbsent(userId, "create-booking-1", "request-hash")).thenReturn(true);
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingStatusLogRepository.save(any(BookingStatusLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        bookingService.createBooking(request, currentUser, "create-booking-1");

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).saveAndFlush(bookingCaptor.capture());
        Booking savedBooking = bookingCaptor.getValue();
        assertEquals(BookingStatus.PENDING, savedBooking.getStatus());
        assertEquals("create-booking-1", savedBooking.getClientRequestId());
        assertEquals("request-hash", savedBooking.getRequestHash());
        assertNotNull(savedBooking.getGuest());
        assertEquals("guest@example.com", savedBooking.getGuest().getEmail());
        assertNotNull(savedBooking.getExpiredAt());
        assertTrue(savedBooking.getExpiredAt().isAfter(beforeCreate.plusMinutes(14)));
        assertTrue(savedBooking.getExpiredAt().isBefore(LocalDateTime.now().plusMinutes(16)));
        verify(inventoryService).holdInventory(
                org.mockito.ArgumentMatchers.eq(savedBooking),
                org.mockito.ArgumentMatchers.eq(savedBooking.getBookedRooms()),
                org.mockito.ArgumentMatchers.eq(savedBooking.getCheckInDateTime().toLocalDate()),
                org.mockito.ArgumentMatchers.eq(savedBooking.getCheckOutDateTime().toLocalDate()),
                org.mockito.ArgumentMatchers.eq(savedBooking.getExpiredAt())
        );
        verify(bookingIdempotencyCacheService).putCompleted(
                eq(userId),
                eq("create-booking-1"),
                eq("request-hash"),
                eq(savedBooking.getId())
        );
    }

    @Test
    void createBookingUpdatesExistingGuestMatchedByIdentityCard() {
        UUID userId = UUID.randomUUID();
        UUID roomTypeId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        RoomType roomType = new RoomType();
        roomType.setId(roomTypeId);
        roomType.setName("Standard");
        roomType.setCode("STD");
        roomType.setMaxOccupancy(2);
        roomType.setBasePrice(BigDecimal.valueOf(850_000));

        Guest existingGuest = new Guest();
        existingGuest.setId(UUID.randomUUID());
        existingGuest.setFirstName("Old");
        existingGuest.setLastName("Guest");
        existingGuest.setIdentifyCardNo("ID123456");
        existingGuest.setPhoneNumber("0900000000");
        existingGuest.setEmail("old@example.com");

        BookingRequest request = new BookingRequest();
        request.setCheckInDate(LocalDateTime.now().plusDays(1));
        request.setCheckOutDate(LocalDateTime.now().plusDays(2));
        BookedRoomRequest bookedRoomRequest = new BookedRoomRequest();
        bookedRoomRequest.setRoomTypeId(roomTypeId);
        bookedRoomRequest.setQuantity(1);
        request.setRooms(List.of(bookedRoomRequest));
        request.setGuest(buildGuestRequest());

        UserPrincipal currentUser = new UserPrincipal(
                userId,
                "User",
                "user",
                "user@example.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                ActorType.USER,
                true
        );

        when(bookingStateMachine.initialStatus()).thenReturn(BookingStatus.PENDING);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roomTypeRepository.findById(roomTypeId)).thenReturn(Optional.of(roomType));
        when(guestRepository.findByIdentifyCardNo("ID123456")).thenReturn(Optional.of(existingGuest));
        when(guestRepository.findByEmail("guest@example.com")).thenReturn(Optional.empty());
        when(guestRepository.save(any(Guest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingRequestHashService.hash(request)).thenReturn("request-hash");
        when(bookingIdempotencyCacheService.find(userId, "create-booking-2")).thenReturn(Optional.empty());
        when(bookingRepository.findByUser_IdAndClientRequestId(userId, "create-booking-2")).thenReturn(Optional.empty());
        when(bookingIdempotencyCacheService.putProcessingIfAbsent(userId, "create-booking-2", "request-hash")).thenReturn(true);
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingStatusLogRepository.save(any(BookingStatusLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        bookingService.createBooking(request, currentUser,"create-booking-2");

        ArgumentCaptor<Guest> guestCaptor = ArgumentCaptor.forClass(Guest.class);
        verify(guestRepository).save(guestCaptor.capture());
        Guest savedGuest = guestCaptor.getValue();
        assertEquals(existingGuest.getId(), savedGuest.getId());
        assertEquals("Jane", savedGuest.getFirstName());
        assertEquals("Doe", savedGuest.getLastName());
        assertEquals("guest@example.com", savedGuest.getEmail());
        verify(guestRepository, never()).findByEmail("old@example.com");
    }

    @Test
    void expirePendingBookingsTransitionsExpiredBookingsAndWritesLog() {
        Booking booking = new Booking();
        booking.setStatus(BookingStatus.PENDING);
        booking.setExpiredAt(LocalDateTime.now().minusMinutes(1));

        when(bookingRepository.findByStatusAndExpiredAtLessThanEqual(
                org.mockito.ArgumentMatchers.eq(BookingStatus.PENDING),
                any(LocalDateTime.class)
        )).thenReturn(List.of(booking));
        when(bookingStatusLogRepository.save(any(BookingStatusLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int expiredCount = bookingService.expirePendingBookings();

        assertEquals(1, expiredCount);
        verify(bookingStateMachine).transition(booking, BookingStatus.EXPIRED);
        verify(bookingRepository).saveAll(org.mockito.ArgumentMatchers.<Collection<Booking>>argThat(bookings -> bookings.contains(booking)));

        ArgumentCaptor<BookingStatusLog> logCaptor = ArgumentCaptor.forClass(BookingStatusLog.class);
        verify(bookingStatusLogRepository).save(logCaptor.capture());
        BookingStatusLog log = logCaptor.getValue();
        assertEquals(BookingStatus.PENDING, log.getFromStatus());
        assertEquals(BookingStatus.EXPIRED, log.getToStatus());
        assertEquals(ActorType.SYSTEM, log.getPerformedByType());
        assertEquals("Booking expired", log.getNote());
        verify(inventoryService).releaseActiveHolds(
                org.mockito.ArgumentMatchers.eq(booking),
                org.mockito.ArgumentMatchers.eq(com.example.bookingapi.features.booking.model.enums.InventoryHoldStatus.EXPIRED)
        );
    }

    private BookingGuestRequest buildGuestRequest() {
        BookingGuestRequest guest = new BookingGuestRequest();
        guest.setFirstName("Jane");
        guest.setLastName("Doe");
        guest.setMiddleName("Q");
        guest.setIdentifyCardNo("ID123456");
        guest.setPhoneNumber("0912345678");
        guest.setEmail("guest@example.com");
        return guest;
    }
}

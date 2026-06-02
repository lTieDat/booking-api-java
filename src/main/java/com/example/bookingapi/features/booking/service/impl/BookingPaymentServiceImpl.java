package com.example.bookingapi.features.booking.service.impl;

import com.example.bookingapi.common.exception.BadRequestException;
import com.example.bookingapi.common.exception.ResourceNotFoundException;
import com.example.bookingapi.common.security.UserPrincipal;
import com.example.bookingapi.features.auth.model.enums.ActorType;
import com.example.bookingapi.features.booking.model.BookedRoom;
import com.example.bookingapi.features.booking.model.Booking;
import com.example.bookingapi.features.booking.model.BookingStatusLog;
import com.example.bookingapi.features.booking.model.InventoryHold;
import com.example.bookingapi.features.booking.model.enums.BookingStatus;
import com.example.bookingapi.features.booking.model.enums.InventoryHoldStatus;
import com.example.bookingapi.features.booking.repository.BookingRepository;
import com.example.bookingapi.features.booking.repository.BookingStatusLogRepository;
import com.example.bookingapi.features.booking.repository.InventoryHoldRepository;
import com.example.bookingapi.features.booking.service.BookingPaymentItem;
import com.example.bookingapi.features.booking.service.BookingPaymentService;
import com.example.bookingapi.features.booking.service.BookingPaymentSnapshot;
import com.example.bookingapi.features.booking.service.BookingStateMachine;
import com.example.bookingapi.features.booking.service.InventoryService;
import com.example.bookingapi.features.payment.model.Payment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class BookingPaymentServiceImpl implements BookingPaymentService {

    private final BookingRepository bookingRepository;
    private final InventoryHoldRepository inventoryHoldRepository;
    private final BookingStatusLogRepository bookingStatusLogRepository;
    private final BookingStateMachine bookingStateMachine;
    private final InventoryService inventoryService;

    public BookingPaymentServiceImpl(
            BookingRepository bookingRepository,
            InventoryHoldRepository inventoryHoldRepository,
            BookingStatusLogRepository bookingStatusLogRepository,
            BookingStateMachine bookingStateMachine,
            InventoryService inventoryService
    ) {
        this.bookingRepository = bookingRepository;
        this.inventoryHoldRepository = inventoryHoldRepository;
        this.bookingStatusLogRepository = bookingStatusLogRepository;
        this.bookingStateMachine = bookingStateMachine;
        this.inventoryService = inventoryService;
    }

    @Override
    public BookingPaymentSnapshot getPendingBookingForPayment(UUID bookingId, UserPrincipal currentUser) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You don't have permission to create payment for this booking");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Only pending bookings can be paid");
        }
        if (booking.getExpiredAt() != null && !booking.getExpiredAt().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Booking payment window has expired");
        }
        if (booking.getTotalPrice() == null || booking.getTotalPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Booking amount must be greater than zero");
        }
        return new BookingPaymentSnapshot(
                booking,
                booking.getId(),
                booking.getTotalPrice(),
                booking.getCurrency(),
                booking.getExpiredAt(),
                toPaymentItems(booking)
        );
    }

    @Override
    @Transactional
    public void attachPaymentToActiveHolds(UUID bookingId, Payment payment) {
        List<InventoryHold> holds = inventoryHoldRepository.findByBooking_IdAndStatus(
                bookingId,
                InventoryHoldStatus.ACTIVE
        );
        for (InventoryHold hold : holds) {
            hold.setPayment(payment);
        }
        inventoryHoldRepository.saveAll(holds);
    }

    @Override
    @Transactional
    public void confirmPaidBooking(UUID bookingId, UUID paymentId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return;
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            return;
        }
        BookingStatus fromStatus = booking.getStatus();
        bookingStateMachine.transition(booking, BookingStatus.CONFIRMED);
        inventoryService.consumeActiveHolds(booking);
        bookingRepository.save(booking);
        recordSystemStatusLog(
                booking,
                fromStatus,
                BookingStatus.CONFIRMED,
                "Booking confirmed by payment " + paymentId
        );
    }

    @Override
    @Transactional
    public void releaseUnpaidBooking(UUID bookingId, UUID paymentId, BookingStatus terminalStatus, String reason) {
        if (terminalStatus != BookingStatus.CANCELLED && terminalStatus != BookingStatus.EXPIRED) {
            throw new BadRequestException("Unpaid booking can only be cancelled or expired");
        }
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        if (booking.getStatus() != BookingStatus.PENDING) {
            return;
        }
        BookingStatus fromStatus = booking.getStatus();
        bookingStateMachine.transition(booking, terminalStatus);
        InventoryHoldStatus holdStatus = terminalStatus == BookingStatus.EXPIRED
                ? InventoryHoldStatus.EXPIRED
                : InventoryHoldStatus.RELEASED;
        inventoryService.releaseActiveHolds(booking, holdStatus);
        bookingRepository.save(booking);
        recordSystemStatusLog(
                booking,
                fromStatus,
                terminalStatus,
                reason == null ? "Booking released by payment " + paymentId : reason
        );
    }

    @Override
    @Transactional
    public void markRefundedBooking(UUID bookingId, UUID refundId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
        if (booking.getStatus() == BookingStatus.REFUNDED) {
            return;
        }
        BookingStatus fromStatus = booking.getStatus();
        bookingStateMachine.transition(booking, BookingStatus.REFUNDED);
        bookingRepository.save(booking);
        recordSystemStatusLog(
                booking,
                fromStatus,
                BookingStatus.REFUNDED,
                reason == null ? "Booking refunded by refund " + refundId : reason
        );
    }

    private List<BookingPaymentItem> toPaymentItems(Booking booking) {
        long nights = ChronoUnit.DAYS.between(
                booking.getCheckInDateTime().toLocalDate(),
                booking.getCheckOutDateTime().toLocalDate()
        );
        return booking.getBookedRooms().stream()
                .map(bookedRoom -> toPaymentItem(bookedRoom, nights))
                .toList();
    }

    private BookingPaymentItem toPaymentItem(BookedRoom bookedRoom, long nights) {
        BigDecimal unitAmount = bookedRoom.getUnitPrice().multiply(BigDecimal.valueOf(nights));
        String name = bookedRoom.getRoomTypeNameSnapshot();
        if (name == null || name.isBlank()) {
            name = "Room " + bookedRoom.getRoomType().getId();
        }
        return new BookingPaymentItem(name, bookedRoom.getQuantity(), unitAmount);
    }

    private void recordSystemStatusLog(
            Booking booking,
            BookingStatus fromStatus,
            BookingStatus toStatus,
            String note
    ) {
        BookingStatusLog log = new BookingStatusLog();
        log.setBooking(booking);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setPerformedByType(ActorType.SYSTEM);
        log.setNote(note);
        bookingStatusLogRepository.save(log);
    }
}

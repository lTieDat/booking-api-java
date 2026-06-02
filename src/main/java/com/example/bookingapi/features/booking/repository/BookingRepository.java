package com.example.bookingapi.features.booking.repository;

import com.example.bookingapi.features.booking.model.Booking;
import com.example.bookingapi.features.booking.model.enums.BookingStatus;
import com.example.bookingapi.features.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    Page<Booking> findByUser(User user, Pageable pageable);
    List<Booking> findByStatusAndExpiredAtLessThanEqual(
            BookingStatus status,
            LocalDateTime expiredAt
    );

    Optional<Booking> findByUser_IdAndClientRequestId(UUID id, String clientRequestId);

    @Query("""
            SELECT DISTINCT b
            FROM Booking b
            JOIN b.bookedRooms br
            JOIN br.roomType rt
            WHERE rt.hotel.id = :hotelId
            """)
    Page<Booking> findDistinctByHotelId(@Param("hotelId") UUID hotelId, Pageable pageable);

    @Query("""
            SELECT DISTINCT b
            FROM Booking b
            JOIN b.bookedRooms br
            JOIN br.roomType rt
            WHERE rt.hotel.id = :hotelId
              AND b.status = :status
            """)
    Page<Booking> findDistinctByHotelIdAndStatus(
            @Param("hotelId") UUID hotelId,
            @Param("status") BookingStatus status,
            Pageable pageable
    );

    @Query(value = """
            SELECT COALESCE(SUM(br.quantity), 0)
            FROM booked_rooms br
            JOIN bookings b ON b.id = br.booking_id
            WHERE br.room_type_id = :roomTypeId
              AND CAST(b.check_in_date_time AS DATE) <= :date
              AND :date < CAST(b.check_out_date_time AS DATE)
              AND b.status IN (:confirmedStatus, :checkedInStatus)
            """, nativeQuery = true)
    long sumBookedQuantityForRoomTypeAndDate(
            @Param("roomTypeId") UUID roomTypeId,
            @Param("date") LocalDate date,
            @Param("confirmedStatus") String confirmedStatus,
            @Param("checkedInStatus") String checkedInStatus
    );

    @Query("""
            SELECT COUNT(DISTINCT b.id)
            FROM Booking b
            JOIN b.bookedRooms br
            JOIN br.roomType rt
            WHERE rt.hotel.id = :hotelId
            """)
    long countDistinctByHotelId(@Param("hotelId") UUID hotelId);

    @Query("""
            SELECT COUNT(DISTINCT b.id)
            FROM Booking b
            JOIN b.bookedRooms br
            JOIN br.roomType rt
            WHERE rt.hotel.id = :hotelId
              AND b.status IN :statuses
            """)
    long countDistinctByHotelIdAndStatuses(
            @Param("hotelId") UUID hotelId,
            @Param("statuses") Collection<BookingStatus> statuses
    );

    @Query("""
            SELECT SUM(b.totalPrice)
            FROM Booking b
            WHERE b.status IN :statuses
              AND EXISTS (
                  SELECT br.id
                  FROM BookedRoom br
                  JOIN br.roomType rt
                  WHERE br.booking = b
                    AND rt.hotel.id = :hotelId
              )
            """)
    BigDecimal sumTotalPriceByHotelIdAndStatuses(
            @Param("hotelId") UUID hotelId,
            @Param("statuses") Collection<BookingStatus> statuses
    );
}

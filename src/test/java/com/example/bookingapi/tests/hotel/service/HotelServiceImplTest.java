package com.example.bookingapi.tests.hotel.service;

import com.example.bookingapi.common.exception.BadRequestException;
import com.example.bookingapi.common.security.UserPrincipal;
import com.example.bookingapi.features.auth.model.Manager;
import com.example.bookingapi.features.auth.model.enums.ActorType;
import com.example.bookingapi.features.auth.repository.ManagerRepository;
import com.example.bookingapi.features.booking.model.enums.BookingStatus;
import com.example.bookingapi.features.booking.repository.BookingRepository;
import com.example.bookingapi.features.hotel.dto.request.HotelSearchRequest;
import com.example.bookingapi.features.hotel.dto.response.ManagerHotelStatsResponse;
import com.example.bookingapi.features.hotel.model.Hotel;
import com.example.bookingapi.features.hotel.dto.response.HotelSearchResponse;
import com.example.bookingapi.features.hotel.model.enums.HotelImageType;
import com.example.bookingapi.features.hotel.repository.HotelRepository;
import com.example.bookingapi.features.hotel.repository.HotelSearchProjection;
import com.example.bookingapi.features.hotel.repository.LocationRepository;
import com.example.bookingapi.features.review.repository.ReviewRepository;
import com.example.bookingapi.features.room.repository.RoomRepository;
import com.example.bookingapi.features.hotel.service.impl.HotelServiceImpl;
import com.example.bookingapi.common.storage.ObjectStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HotelServiceImplTest {

    @Mock private HotelRepository hotelRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private ObjectStorageService objectStorageService;
    @Mock private ManagerRepository managerRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private ReviewRepository reviewRepository;

    @InjectMocks private HotelServiceImpl hotelService;

    @Test
    void searchHotelsRejectsInvalidDateRange() {
        HotelSearchRequest request = buildRequest();
        request.setCheckIn(LocalDate.now().plusDays(3));
        request.setCheckOut(LocalDate.now().plusDays(2));

        assertThrows(BadRequestException.class, () -> hotelService.searchHotels(request));
        verifyNoInteractions(hotelRepository);
    }

    @Test
    void searchHotelsCapsPageSizeAndMapsProjection() {
        UUID hotelId = UUID.randomUUID();
        HotelSearchRequest request = buildRequest();
        request.setPage(2);
        request.setSize(999);
        request.setKeyword("district 1");

        HotelSearchProjection projection = buildProjection(hotelId);
        when(hotelRepository.searchAvailableHotels(
                eq(new BigDecimal("10.776900")),
                eq(new BigDecimal("106.700900")),
                eq(new BigDecimal("15.0")),
                eq(request.getCheckIn()),
                eq(request.getCheckOut()),
                eq(2),
                eq(1),
                eq(1),
                eq("district 1"),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(projection), PageRequest.of(2, 50), 51));

        var response = hotelService.searchHotels(request);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        org.mockito.Mockito.verify(hotelRepository).searchAvailableHotels(
                eq(new BigDecimal("10.776900")),
                eq(new BigDecimal("106.700900")),
                eq(new BigDecimal("15.0")),
                eq(request.getCheckIn()),
                eq(request.getCheckOut()),
                eq(2),
                eq(1),
                eq(1),
                eq("district 1"),
                pageableCaptor.capture()
        );

        assertEquals(2, pageableCaptor.getValue().getPageNumber());
        assertEquals(50, pageableCaptor.getValue().getPageSize());
        assertEquals(1, response.getContent().size());

        HotelSearchResponse hotel = response.getContent().getFirst();
        assertEquals(hotelId, hotel.getId());
        assertEquals("Grand Palace Hotel", hotel.getName());
        assertEquals(new BigDecimal("4.80"), hotel.getRatingAvg());
        assertEquals(124L, hotel.getReviewCount());
        assertEquals(new BigDecimal("850000.00"), hotel.getMinPrice());
        assertEquals(new BigDecimal("1.25"), hotel.getDistanceKm());
        assertEquals(new BigDecimal("10.776889"), hotel.getLocation().getLatitude());
        assertEquals(new BigDecimal("106.700806"), hotel.getLocation().getLongitude());
        assertEquals(HotelImageType.PREVIEW, hotel.getPreviewImage().getImageType());
        assertEquals(2, response.getPage());
        assertEquals(50, response.getSize());
        assertEquals(101, response.getTotalElements());
        assertEquals(3, response.getTotalPages());
    }

    @Test
    void getManagerHotelStatsBuildsOverviewFromAssignedHotel() {
        UUID managerId = UUID.randomUUID();
        UUID hotelId = UUID.randomUUID();
        Hotel hotel = new Hotel();
        hotel.setId(hotelId);
        hotel.setName("Grand Palace Hotel");

        Manager manager = new Manager();
        manager.setId(managerId);
        manager.setHotel(hotel);

        UserPrincipal currentUser = new UserPrincipal(
                managerId,
                "Operations Manager",
                "manager@booking.local",
                "manager@booking.local",
                "secret",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
                ActorType.MANAGER,
                true
        );

        when(managerRepository.findById(managerId)).thenReturn(java.util.Optional.of(manager));
        when(roomRepository.countByRoomType_Hotel_IdAndIsActiveTrue(hotelId)).thenReturn(25L);
        when(bookingRepository.countDistinctByHotelId(hotelId)).thenReturn(80L);
        when(bookingRepository.countDistinctByHotelIdAndStatuses(hotelId, List.of(BookingStatus.CONFIRMED))).thenReturn(12L);
        when(bookingRepository.countDistinctByHotelIdAndStatuses(hotelId, List.of(BookingStatus.CHECKED_IN))).thenReturn(4L);
        when(bookingRepository.countDistinctByHotelIdAndStatuses(hotelId, List.of(BookingStatus.CHECKED_OUT))).thenReturn(46L);
        when(bookingRepository.countDistinctByHotelIdAndStatuses(
                hotelId, List.of(BookingStatus.CANCELLED, BookingStatus.NO_SHOW, BookingStatus.EXPIRED, BookingStatus.REFUNDED)
        )).thenReturn(18L);
        when(bookingRepository.sumTotalPriceByHotelIdAndStatuses(
                hotelId, List.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN, BookingStatus.CHECKED_OUT, BookingStatus.REFUNDED)
        )).thenReturn(new BigDecimal("125000000.00"));
        when(reviewRepository.countByHotel_IdAndIsVisibleTrue(hotelId)).thenReturn(137L);
        when(reviewRepository.getAverageVisibleRatingByHotelId(hotelId)).thenReturn(4.678d);

        ManagerHotelStatsResponse response = hotelService.getManagerHotelStats(currentUser);

        assertEquals(hotelId, response.getHotelId());
        assertEquals("Grand Palace Hotel", response.getHotelName());
        assertEquals(25L, response.getTotalRooms());
        assertEquals(80L, response.getTotalBookings());
        assertEquals(12L, response.getUpcomingBookings());
        assertEquals(4L, response.getCheckedInBookings());
        assertEquals(46L, response.getCompletedBookings());
        assertEquals(18L, response.getCancelledBookings());
        assertEquals(new BigDecimal("125000000.00"), response.getTotalRevenue());
        assertEquals(new BigDecimal("4.68"), response.getAverageRating());
        assertEquals(137L, response.getReviewCount());
    }

    private HotelSearchRequest buildRequest() {
        HotelSearchRequest request = new HotelSearchRequest();
        request.setLatitude(new BigDecimal("10.776900"));
        request.setLongitude(new BigDecimal("106.700900"));
        request.setRadiusKm(new BigDecimal("15.0"));
        request.setCheckIn(LocalDate.now().plusDays(1));
        request.setCheckOut(LocalDate.now().plusDays(3));
        request.setAdults(2);
        request.setChildren(1);
        request.setRoomCount(1);
        return request;
    }

    private HotelSearchProjection buildProjection(UUID hotelId) {
        return new HotelSearchProjection() {
            @Override
            public UUID getId() {
                return hotelId;
            }

            @Override
            public String getName() {
                return "Grand Palace Hotel";
            }

            @Override
            public String getDescription() {
                return "Central business hotel";
            }

            @Override
            public String getCountry() {
                return "Vietnam";
            }

            @Override
            public String getCity() {
                return "Ho Chi Minh City";
            }

            @Override
            public String getProvince() {
                return null;
            }

            @Override
            public String getDistrict() {
                return "District 1";
            }

            @Override
            public String getDetail() {
                return "123 Nguyen Hue Street";
            }

            @Override
            public BigDecimal getLatitude() {
                return new BigDecimal("10.776889");
            }

            @Override
            public BigDecimal getLongitude() {
                return new BigDecimal("106.700806");
            }

            @Override
            public String getPreviewImageUrl() {
                return "https://example.com/grand-palace.jpg";
            }

            @Override
            public String getPreviewImageBucket() {
                return "booking-assets";
            }

            @Override
            public String getPreviewImageObjectKey() {
                return "grand-palace.jpg";
            }

            @Override
            public String getPreviewImageContentType() {
                return "image/jpeg";
            }

            @Override
            public Long getPreviewImageSizeBytes() {
                return 2048L;
            }

            @Override
            public String getPreviewImageAltText() {
                return "Grand Palace preview";
            }

            @Override
            public BigDecimal getDistanceKm() {
                return new BigDecimal("1.25");
            }

            @Override
            public BigDecimal getRatingAvg() {
                return new BigDecimal("4.80");
            }

            @Override
            public Long getReviewCount() {
                return 124L;
            }

            @Override
            public BigDecimal getMinPrice() {
                return new BigDecimal("850000.00");
            }
        };
    }
}

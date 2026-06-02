package com.example.bookingapi.features.hotel.service.impl;

import com.example.bookingapi.common.exception.BadRequestException;
import com.example.bookingapi.common.exception.ResourceNotFoundException;
import com.example.bookingapi.common.response.PagedResponse;
import com.example.bookingapi.common.security.UserPrincipal;
import com.example.bookingapi.common.storage.ObjectStorageService;
import com.example.bookingapi.common.upload.UploadFileResponse;
import com.example.bookingapi.common.util.AppConstants;
import com.example.bookingapi.features.auth.model.Manager;
import com.example.bookingapi.features.auth.repository.ManagerRepository;
import com.example.bookingapi.features.booking.model.enums.BookingStatus;
import com.example.bookingapi.features.booking.repository.BookingRepository;
import com.example.bookingapi.features.hotel.dto.request.HotelSearchRequest;
import com.example.bookingapi.features.hotel.model.Hotel;
import com.example.bookingapi.features.hotel.model.HotelImage;
import com.example.bookingapi.features.hotel.model.Location;
import com.example.bookingapi.features.hotel.model.enums.HotelImageType;
import com.example.bookingapi.features.hotel.dto.request.HotelImageRequest;
import com.example.bookingapi.features.hotel.dto.request.HotelRequest;
import com.example.bookingapi.features.hotel.dto.request.LocationRequest;
import com.example.bookingapi.common.response.ApiMessageResponse;
import com.example.bookingapi.features.hotel.dto.response.HotelImageResponse;
import com.example.bookingapi.features.hotel.dto.response.ManagerHotelStatsResponse;
import com.example.bookingapi.features.hotel.dto.response.HotelResponse;
import com.example.bookingapi.features.hotel.dto.response.HotelSearchResponse;
import com.example.bookingapi.features.hotel.dto.response.LocationResponse;
import com.example.bookingapi.features.hotel.repository.HotelSearchProjection;
import com.example.bookingapi.features.hotel.repository.HotelRepository;
import com.example.bookingapi.features.hotel.repository.LocationRepository;
import com.example.bookingapi.features.hotel.service.HotelService;
import com.example.bookingapi.features.review.repository.ReviewRepository;
import com.example.bookingapi.features.room.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class HotelServiceImpl implements HotelService {

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private ObjectStorageService objectStorageService;

    @Autowired
    private ManagerRepository managerRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Override
    public PagedResponse<HotelResponse> getAllHotels(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Hotel> hotels = keyword == null || keyword.isBlank()
                ? hotelRepository.findAll(pageable)
                : hotelRepository.searchByNameOrLocation(keyword.trim(), pageable);
        List<HotelResponse> content = hotels.getContent().stream()
                .map(this::toResponse)
                .toList();
        return new PagedResponse<>(content, hotels.getNumber(), hotels.getSize(),
                hotels.getTotalElements(), hotels.getTotalPages(), hotels.isLast());
    }

    @Override
    public PagedResponse<HotelSearchResponse> searchHotels(HotelSearchRequest request) {
        validateSearchRequest(request);

        int resolvedPage = resolvePage(request.getPage());
        int resolvedSize = resolveSize(request.getSize());
        Pageable pageable = PageRequest.of(resolvedPage, resolvedSize);
        Page<HotelSearchProjection> hotels = hotelRepository.searchAvailableHotels(
                request.getLatitude(),
                request.getLongitude(),
                request.getRadiusKm(),
                request.getCheckIn(),
                request.getCheckOut(),
                request.getAdults(),
                request.getChildren(),
                request.getRoomCount(),
                normalizeKeyword(request.getKeyword()),
                pageable
        );

        List<HotelSearchResponse> content = hotels.getContent().stream()
                .map(this::toSearchResponse)
                .toList();
        return new PagedResponse<>(content, hotels.getNumber(), hotels.getSize(),
                hotels.getTotalElements(), hotels.getTotalPages(), hotels.isLast());
    }

    @Override
    public ManagerHotelStatsResponse getManagerHotelStats(UserPrincipal currentUser) {
        Manager manager = managerRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Manager", "id", currentUser.getId()));
        if (manager.getHotel() == null) {
            throw new BadRequestException("Manager is not assigned to any hotel");
        }

        UUID hotelId = manager.getHotel().getId();
        long totalRooms = roomRepository.countByRoomType_Hotel_IdAndIsActiveTrue(hotelId);
        long totalBookings = bookingRepository.countDistinctByHotelId(hotelId);
        long upcomingBookings = bookingRepository.countDistinctByHotelIdAndStatuses(
                hotelId, List.of(BookingStatus.CONFIRMED)
        );
        long checkedInBookings = bookingRepository.countDistinctByHotelIdAndStatuses(
                hotelId, List.of(BookingStatus.CHECKED_IN)
        );
        long completedBookings = bookingRepository.countDistinctByHotelIdAndStatuses(
                hotelId, List.of(BookingStatus.CHECKED_OUT)
        );
        long cancelledBookings = bookingRepository.countDistinctByHotelIdAndStatuses(
                hotelId, List.of(BookingStatus.CANCELLED, BookingStatus.NO_SHOW, BookingStatus.EXPIRED, BookingStatus.REFUNDED)
        );
        BigDecimal totalRevenue = bookingRepository.sumTotalPriceByHotelIdAndStatuses(
                hotelId, List.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN, BookingStatus.CHECKED_OUT, BookingStatus.REFUNDED)
        );
        long reviewCount = reviewRepository.countByHotel_IdAndIsVisibleTrue(hotelId);
        Double averageRating = reviewRepository.getAverageVisibleRatingByHotelId(hotelId);

        return new ManagerHotelStatsResponse(
                hotelId,
                manager.getHotel().getName(),
                totalRooms,
                totalBookings,
                upcomingBookings,
                checkedInBookings,
                completedBookings,
                cancelledBookings,
                totalRevenue == null ? BigDecimal.ZERO : totalRevenue,
                averageRating == null
                        ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.valueOf(averageRating).setScale(2, RoundingMode.HALF_UP),
                reviewCount
        );
    }

    @Override
    public HotelResponse getHotel(UUID id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", id));
        return toResponse(hotel);
    }

    @Override
    @Transactional
    public HotelResponse addHotel(HotelRequest hotelRequest) {
        Location location = buildLocation(new Location(), hotelRequest);
        locationRepository.save(location);

        Hotel hotel = new Hotel();
        hotel.setName(hotelRequest.getName());
        hotel.setDescription(hotelRequest.getDescription());
        hotel.setLocation(location);
        if (hotelRequest.getPreviewImage() != null) {
            applyPreviewImage(hotel, hotelRequest.getPreviewImage());
        }
        return toResponse(hotelRepository.save(hotel));
    }

    @Override
    @Transactional
    public HotelResponse updateHotel(UUID id, HotelRequest hotelRequest) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", id));

        Location location = hotel.getLocation() != null ? hotel.getLocation() : new Location();
        buildLocation(location, hotelRequest);
        locationRepository.save(location);

        hotel.setName(hotelRequest.getName());
        hotel.setDescription(hotelRequest.getDescription());
        hotel.setLocation(location);
        if (hotelRequest.getPreviewImage() != null) {
            applyPreviewImage(hotel, hotelRequest.getPreviewImage());
        }
        return toResponse(hotelRepository.save(hotel));
    }

    @Override
    @Transactional
    public HotelResponse uploadPreviewImage(UUID id, MultipartFile file, String altText) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", id));

        String folder = "hotel-images/" + id;
        String resolvedAltText = (altText == null || altText.isBlank())
                ? hotel.getName() + " preview"
                : altText.trim();

        UploadFileResponse uploaded = objectStorageService.upload(file, folder);
        applyPreviewImage(
                hotel,
                uploaded.getUrl(),
                resolvedAltText,
                uploaded.getBucket(),
                uploaded.getObjectKey(),
                uploaded.getContentType(),
                uploaded.getSize()
        );
        return toResponse(hotelRepository.save(hotel));
    }

    @Override
    @Transactional
    public ApiMessageResponse deletePreviewImage(UUID id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", id));

        HotelImage previewImage = hotel.getImages().stream()
                .filter(image -> image.getImageType() == HotelImageType.PREVIEW)
                .findFirst()
                .orElse(null);

        if (previewImage == null) {
            return new ApiMessageResponse(true, "Hotel preview image already absent");
        }

        if (hasObjectStorageMetadata(previewImage)) {
            objectStorageService.delete(previewImage.getBucket(), previewImage.getObjectKey());
        }

        hotel.getImages().remove(previewImage);
        hotelRepository.save(hotel);
        return new ApiMessageResponse(true, "Hotel preview image deleted successfully");
    }

    @Override
    @Transactional
    public ApiMessageResponse deleteHotel(UUID id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", id));
        hotelRepository.delete(hotel);
        return new ApiMessageResponse(true, "Hotel deleted successfully");
    }

    private Location buildLocation(Location location, HotelRequest hotelRequest) {
        LocationRequest locationRequest = hotelRequest.getLocation();
        location.setCountry(locationRequest.getCountry());
        location.setCity(locationRequest.getCity());
        location.setProvince(locationRequest.getProvince());
        location.setDistrict(locationRequest.getDistrict());
        location.setDetail(locationRequest.getDetail());
        location.setLatitude(locationRequest.getLatitude());
        location.setLongitude(locationRequest.getLongitude());
        return location;
    }

    private void applyPreviewImage(Hotel hotel, HotelImageRequest previewImageRequest) {
        applyPreviewImage(
                hotel,
                previewImageRequest.getUrl(),
                previewImageRequest.getAltText(),
                previewImageRequest.getBucket(),
                previewImageRequest.getObjectKey(),
                previewImageRequest.getContentType(),
                previewImageRequest.getSize()
        );
    }

    private void applyPreviewImage(
            Hotel hotel,
            String url,
            String altText,
            String bucket,
            String objectKey,
            String contentType,
            Long sizeBytes
    ) {
        HotelImage previewImage = hotel.getImages().stream()
                .filter(image -> image.getImageType() == HotelImageType.PREVIEW)
                .findFirst()
                .orElseGet(() -> {
                    HotelImage image = new HotelImage();
                    image.setHotel(hotel);
                    image.setImageType(HotelImageType.PREVIEW);
                    hotel.getImages().add(image);
                    return image;
                });

        boolean sameUrl = Objects.equals(previewImage.getUrl(), url);
        boolean hasNewObjectStorageMetadata = hasObjectStorageMetadata(bucket, objectKey);

        String resolvedBucket = bucket;
        String resolvedObjectKey = objectKey;
        String resolvedContentType = contentType;
        Long resolvedSizeBytes = sizeBytes;

        if (sameUrl && !hasNewObjectStorageMetadata) {
            resolvedBucket = previewImage.getBucket();
            resolvedObjectKey = previewImage.getObjectKey();
            resolvedContentType = previewImage.getContentType();
            resolvedSizeBytes = previewImage.getSizeBytes();
        }

        deleteReplacedPreviewObject(previewImage, url, resolvedBucket, resolvedObjectKey);

        previewImage.setUrl(url);
        previewImage.setAltText(altText);
        previewImage.setBucket(resolvedBucket);
        previewImage.setObjectKey(resolvedObjectKey);
        previewImage.setContentType(resolvedContentType);
        previewImage.setSizeBytes(resolvedSizeBytes);
    }

    private boolean hasObjectStorageMetadata(HotelImage image) {
        return hasObjectStorageMetadata(image.getBucket(), image.getObjectKey());
    }

    private boolean hasObjectStorageMetadata(String bucket, String objectKey) {
        return bucket != null && !bucket.isBlank()
                && objectKey != null && !objectKey.isBlank();
    }

    private void deleteReplacedPreviewObject(HotelImage currentImage, String newUrl, String newBucket, String newObjectKey) {
        if (!hasObjectStorageMetadata(currentImage)) {
            return;
        }

        boolean sameObject = Objects.equals(currentImage.getBucket(), newBucket)
                && Objects.equals(currentImage.getObjectKey(), newObjectKey);
        boolean sameUrl = Objects.equals(currentImage.getUrl(), newUrl);
        if (!sameObject && !sameUrl) {
            objectStorageService.delete(currentImage.getBucket(), currentImage.getObjectKey());
        }
    }

    private HotelResponse toResponse(Hotel hotel) {
        return new HotelResponse(
                hotel.getId(),
                hotel.getName(),
                hotel.getDescription(),
                toLocationResponse(hotel.getLocation()),
                hotel.getImages().stream()
                        .filter(image -> image.getImageType() == HotelImageType.PREVIEW)
                        .findFirst()
                        .map(this::toImageResponse)
                        .orElse(null)
        );
    }

    private HotelSearchResponse toSearchResponse(HotelSearchProjection projection) {
        return new HotelSearchResponse(
                projection.getId(),
                projection.getName(),
                projection.getDescription(),
                toLocationResponse(
                        projection.getCountry(),
                        projection.getCity(),
                        projection.getProvince(),
                        projection.getDistrict(),
                        projection.getDetail(),
                        projection.getLatitude(),
                        projection.getLongitude()
                ),
                toPreviewImageResponse(projection),
                projection.getDistanceKm(),
                projection.getRatingAvg(),
                projection.getReviewCount() == null ? 0L : projection.getReviewCount(),
                projection.getMinPrice()
        );
    }

    private LocationResponse toLocationResponse(Location location) {
        if (location == null) {
            return null;
        }
        return toLocationResponse(
                location.getCountry(),
                location.getCity(),
                location.getProvince(),
                location.getDistrict(),
                location.getDetail(),
                location.getLatitude(),
                location.getLongitude()
        );
    }

    private LocationResponse toLocationResponse(
            String country,
            String city,
            String province,
            String district,
            String detail,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        return new LocationResponse(country, city, province, district, detail, latitude, longitude);
    }

    private HotelImageResponse toImageResponse(HotelImage hotelImage) {
        return new HotelImageResponse(
                hotelImage.getUrl(),
                hotelImage.getBucket(),
                hotelImage.getObjectKey(),
                hotelImage.getContentType(),
                hotelImage.getSizeBytes(),
                hotelImage.getAltText(),
                hotelImage.getImageType()
        );
    }

    private HotelImageResponse toPreviewImageResponse(HotelSearchProjection projection) {
        if (projection.getPreviewImageUrl() == null || projection.getPreviewImageUrl().isBlank()) {
            return null;
        }
        return new HotelImageResponse(
                projection.getPreviewImageUrl(),
                projection.getPreviewImageBucket(),
                projection.getPreviewImageObjectKey(),
                projection.getPreviewImageContentType(),
                projection.getPreviewImageSizeBytes(),
                projection.getPreviewImageAltText(),
                HotelImageType.PREVIEW
        );
    }

    private void validateSearchRequest(HotelSearchRequest request) {
        if (!request.getCheckIn().isBefore(request.getCheckOut())) {
            throw new BadRequestException("Check-in date must be before check-out date");
        }
        if (request.getAdults() == null || request.getAdults() < 1) {
            throw new BadRequestException("Adults must be at least 1");
        }
        if (request.getChildren() == null || request.getChildren() < 0) {
            throw new BadRequestException("Children must be 0 or greater");
        }
        if (request.getRoomCount() == null || request.getRoomCount() < 1) {
            throw new BadRequestException("Room count must be at least 1");
        }
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    private int resolvePage(Integer page) {
        return page == null || page < 0 ? Integer.parseInt(AppConstants.DEFAULT_PAGE_NUMBER) : page;
    }

    private int resolveSize(Integer size) {
        int resolved = size == null || size < 1
                ? Integer.parseInt(AppConstants.DEFAULT_PAGE_SIZE)
                : size;
        return Math.min(resolved, AppConstants.MAX_PAGE_SIZE);
    }
}

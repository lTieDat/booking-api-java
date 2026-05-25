package com.example.bookingapi.service.impl;

import com.example.bookingapi.exception.ResourceNotFoundException;
import com.example.bookingapi.model.Hotel;
import com.example.bookingapi.model.HotelImage;
import com.example.bookingapi.model.Location;
import com.example.bookingapi.model.enums.HotelImageType;
import com.example.bookingapi.payload.request.HotelImageRequest;
import com.example.bookingapi.payload.request.HotelRequest;
import com.example.bookingapi.payload.request.LocationRequest;
import com.example.bookingapi.payload.response.ApiResponse;
import com.example.bookingapi.payload.response.HotelImageResponse;
import com.example.bookingapi.payload.response.HotelResponse;
import com.example.bookingapi.payload.response.LocationResponse;
import com.example.bookingapi.payload.response.PagedResponse;
import com.example.bookingapi.payload.response.UploadFileResponse;
import com.example.bookingapi.repository.HotelRepository;
import com.example.bookingapi.repository.LocationRepository;
import com.example.bookingapi.service.HotelService;
import com.example.bookingapi.service.ObjectStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

    @Override
    public PagedResponse<HotelResponse> getAllHotels(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Hotel> hotels = hotelRepository.findAll(pageable);
        List<HotelResponse> content = hotels.getContent().stream()
                .map(this::toResponse)
                .toList();
        return new PagedResponse<>(content, hotels.getNumber(), hotels.getSize(),
                hotels.getTotalElements(), hotels.getTotalPages(), hotels.isLast());
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
    public ApiResponse deletePreviewImage(UUID id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", id));

        HotelImage previewImage = hotel.getImages().stream()
                .filter(image -> image.getImageType() == HotelImageType.PREVIEW)
                .findFirst()
                .orElse(null);

        if (previewImage == null) {
            return new ApiResponse(true, "Hotel preview image already absent");
        }

        if (hasObjectStorageMetadata(previewImage)) {
            objectStorageService.delete(previewImage.getBucket(), previewImage.getObjectKey());
        }

        hotel.getImages().remove(previewImage);
        hotelRepository.save(hotel);
        return new ApiResponse(true, "Hotel preview image deleted successfully");
    }

    @Override
    @Transactional
    public ApiResponse deleteHotel(UUID id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", id));
        hotelRepository.delete(hotel);
        return new ApiResponse(true, "Hotel deleted successfully");
    }

    private Location buildLocation(Location location, HotelRequest hotelRequest) {
        LocationRequest locationRequest = hotelRequest.getLocation();
        location.setCountry(locationRequest.getCountry());
        location.setCity(locationRequest.getCity());
        location.setProvince(locationRequest.getProvince());
        location.setDistrict(locationRequest.getDistrict());
        location.setDetail(locationRequest.getDetail());
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

    private LocationResponse toLocationResponse(Location location) {
        if (location == null) {
            return null;
        }
        return new LocationResponse(
                location.getCountry(),
                location.getCity(),
                location.getProvince(),
                location.getDistrict(),
                location.getDetail()
        );
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
}

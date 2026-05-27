package com.example.bookingapi.features.room.service.impl;

import com.example.bookingapi.common.exception.ResourceNotFoundException;
import com.example.bookingapi.features.room.model.Amenity;
import com.example.bookingapi.features.hotel.model.Hotel;
import com.example.bookingapi.features.room.model.RoomType;
import com.example.bookingapi.features.room.dto.request.AmenityRequest;
import com.example.bookingapi.features.room.dto.request.RoomTypeRequest;
import com.example.bookingapi.features.room.dto.response.AmenityResponse;
import com.example.bookingapi.common.response.ApiMessageResponse;
import com.example.bookingapi.features.room.dto.response.RoomTypeResponse;
import com.example.bookingapi.features.room.repository.AmenityRepository;
import com.example.bookingapi.features.hotel.repository.HotelRepository;
import com.example.bookingapi.features.room.repository.RoomTypeRepository;
import com.example.bookingapi.features.room.service.RoomTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RoomTypeServiceImpl implements RoomTypeService {

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private AmenityRepository amenityRepository;

    @Override
    public List<RoomTypeResponse> getRoomTypesByHotel(UUID hotelId) {
        ensureHotelExists(hotelId);
        return roomTypeRepository.findByHotelId(hotelId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public RoomTypeResponse getRoomType(UUID hotelId, UUID roomTypeId) {
        return toResponse(findRoomType(hotelId, roomTypeId));
    }

    @Override
    @Transactional
    public RoomTypeResponse addRoomType(UUID hotelId, RoomTypeRequest request) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", hotelId));

        RoomType roomType = new RoomType();
        roomType.setHotel(hotel);
        applyRequest(roomType, request);
        return toResponse(roomTypeRepository.save(roomType));
    }

    @Override
    @Transactional
    public RoomTypeResponse updateRoomType(UUID hotelId, UUID roomTypeId, RoomTypeRequest request) {
        RoomType roomType = findRoomType(hotelId, roomTypeId);
        applyRequest(roomType, request);
        return toResponse(roomTypeRepository.save(roomType));
    }

    @Override
    @Transactional
    public ApiMessageResponse deleteRoomType(UUID hotelId, UUID roomTypeId) {
        RoomType roomType = findRoomType(hotelId, roomTypeId);
        roomTypeRepository.delete(roomType);
        return new ApiMessageResponse(true, "Room type deleted successfully");
    }

    @Override
    @Transactional
    public AmenityResponse addAmenity(UUID hotelId, UUID roomTypeId, AmenityRequest request) {
        RoomType roomType = findRoomType(hotelId, roomTypeId);
        String code = generateAmenityCode(roomTypeId, request.getName());

        Amenity amenity = new Amenity();
        amenity.setRoomType(roomType);
        amenity.setCode(code);
        applyAmenityRequest(amenity, request);
        return toAmenityResponse(amenityRepository.save(amenity));
    }

    @Override
    public List<AmenityResponse> getAmenities(UUID hotelId, UUID roomTypeId) {
        findRoomType(hotelId, roomTypeId);
        return amenityRepository.findByRoomTypeId(roomTypeId).stream()
                .map(this::toAmenityResponse)
                .toList();
    }

    @Override
    public AmenityResponse getAmenity(UUID hotelId, UUID roomTypeId, UUID amenityId) {
        return toAmenityResponse(findAmenity(hotelId, roomTypeId, amenityId));
    }

    @Override
    @Transactional
    public AmenityResponse updateAmenity(UUID hotelId, UUID roomTypeId, UUID amenityId, AmenityRequest request) {
        Amenity amenity = findAmenity(hotelId, roomTypeId, amenityId);

        applyAmenityRequest(amenity, request);
        return toAmenityResponse(amenityRepository.save(amenity));
    }

    @Override
    @Transactional
    public ApiMessageResponse deleteAmenity(UUID hotelId, UUID roomTypeId, UUID amenityId) {
        Amenity amenity = findAmenity(hotelId, roomTypeId, amenityId);
        amenityRepository.delete(amenity);
        return new ApiMessageResponse(true, "Amenity deleted successfully");
    }

    private void applyRequest(RoomType roomType, RoomTypeRequest request) {
        roomType.setName(request.getName());
        roomType.setCode(request.getCode());
        roomType.setMaxAdults(request.getMaxAdults());
        roomType.setMaxChildren(request.getMaxChildren());
        roomType.setMaxOccupancy(request.getMaxOccupancy());
        roomType.setBedType(request.getBedType());
        roomType.setDescription(request.getDescription());
        roomType.setBasePrice(request.getBasePrice());
        roomType.setIsActive(request.getIsActive() == null || request.getIsActive());
    }

    private void ensureHotelExists(UUID hotelId) {
        if (!hotelRepository.existsById(hotelId)) {
            throw new ResourceNotFoundException("Hotel", "id", hotelId);
        }
    }

    private RoomType findRoomType(UUID hotelId, UUID roomTypeId) {
        return roomTypeRepository.findByIdAndHotelId(roomTypeId, hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("RoomType", "id", roomTypeId));
    }

    private Amenity findAmenity(UUID hotelId, UUID roomTypeId, UUID amenityId) {
        findRoomType(hotelId, roomTypeId);
        return amenityRepository.findByIdAndRoomTypeId(amenityId, roomTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Amenity", "id", amenityId));
    }

    private void applyAmenityRequest(Amenity amenity, AmenityRequest request) {
        amenity.setName(request.getName().trim());
        amenity.setDescription(request.getDescription());
        amenity.setQuantity(request.getQuantity() == null ? 1 : request.getQuantity());
        amenity.setIsActive(request.getIsActive() == null || request.getIsActive());
    }

    private String generateAmenityCode(UUID roomTypeId, String name) {
        String baseCode = normalizeCode(name);
        String code = baseCode;
        int suffix = 2;

        while (amenityRepository.existsByRoomTypeIdAndCode(roomTypeId, code)) {
            code = baseCode + "-" + suffix;
            suffix++;
        }

        return code;
    }

    private String normalizeCode(String value) {
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    private RoomTypeResponse toResponse(RoomType roomType) {
        return new RoomTypeResponse(
                roomType.getId(),
                roomType.getHotel().getId(),
                roomType.getName(),
                roomType.getCode(),
                roomType.getMaxAdults(),
                roomType.getMaxChildren(),
                roomType.getMaxOccupancy(),
                roomType.getBedType(),
                roomType.getDescription(),
                roomType.getBasePrice(),
                roomType.getAmenities().stream()
                        .map(this::toAmenityResponse)
                        .toList(),
                roomType.getIsActive()
        );
    }

    private AmenityResponse toAmenityResponse(Amenity amenity) {
        return new AmenityResponse(
                amenity.getId(),
                amenity.getCode(),
                amenity.getName(),
                amenity.getDescription(),
                amenity.getQuantity(),
                amenity.getIsActive()
        );
    }
}

package com.example.bookingapi.features.room.service;

import com.example.bookingapi.features.room.dto.request.AmenityRequest;
import com.example.bookingapi.features.room.dto.request.RoomTypeRequest;
import com.example.bookingapi.features.room.dto.response.AmenityResponse;
import com.example.bookingapi.common.response.ApiMessageResponse;
import com.example.bookingapi.features.room.dto.response.RoomTypeResponse;

import java.util.List;
import java.util.UUID;

public interface RoomTypeService {
    List<RoomTypeResponse> getRoomTypesByHotel(UUID hotelId);

    RoomTypeResponse getRoomType(UUID hotelId, UUID roomTypeId);

    RoomTypeResponse addRoomType(UUID hotelId, RoomTypeRequest request);

    RoomTypeResponse updateRoomType(UUID hotelId, UUID roomTypeId, RoomTypeRequest request);

    ApiMessageResponse deleteRoomType(UUID hotelId, UUID roomTypeId);

    List<AmenityResponse> getAmenities(UUID hotelId, UUID roomTypeId);

    AmenityResponse getAmenity(UUID hotelId, UUID roomTypeId, UUID amenityId);

    AmenityResponse addAmenity(UUID hotelId, UUID roomTypeId, AmenityRequest request);

    AmenityResponse updateAmenity(UUID hotelId, UUID roomTypeId, UUID amenityId, AmenityRequest request);

    ApiMessageResponse deleteAmenity(UUID hotelId, UUID roomTypeId, UUID amenityId);
}

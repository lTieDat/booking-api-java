package com.example.bookingapi.features.room.service;

import com.example.bookingapi.features.room.dto.request.RoomRequest;
import com.example.bookingapi.common.response.ApiMessageResponse;
import com.example.bookingapi.features.room.dto.response.RoomResponse;

import java.util.List;
import java.util.UUID;

public interface RoomService {
    List<RoomResponse> getRoomsByHotel(UUID hotelId);
    RoomResponse getRoom(UUID hotelId, UUID roomId);
    RoomResponse addRoom(UUID hotelId, RoomRequest roomRequest);
    RoomResponse updateRoom(UUID hotelId, UUID roomId, RoomRequest roomRequest);
    ApiMessageResponse deleteRoom(UUID hotelId, UUID roomId);
}

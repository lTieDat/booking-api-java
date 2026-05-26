package com.example.bookingapi.service;

import com.example.bookingapi.payload.request.RoomRequest;
import com.example.bookingapi.payload.response.ApiResponse;
import com.example.bookingapi.payload.response.RoomResponse;

import java.util.List;
import java.util.UUID;

public interface RoomService {
    List<RoomResponse> getRoomsByHotel(UUID hotelId);
    RoomResponse getRoom(UUID hotelId, UUID roomId);
    RoomResponse addRoom(UUID hotelId, RoomRequest roomRequest);
    RoomResponse updateRoom(UUID hotelId, UUID roomId, RoomRequest roomRequest);
    ApiResponse deleteRoom(UUID hotelId, UUID roomId);
}

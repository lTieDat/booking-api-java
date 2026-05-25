package com.example.bookingapi.service;

import com.example.bookingapi.model.Room;
import com.example.bookingapi.payload.request.RoomRequest;
import com.example.bookingapi.payload.response.ApiResponse;

import java.util.List;
import java.util.UUID;

public interface RoomService {
    List<Room> getRoomsByHotel(UUID hotelId);
    Room getRoom(UUID hotelId, UUID roomId);
    Room addRoom(UUID hotelId, RoomRequest roomRequest);
    Room updateRoom(UUID hotelId, UUID roomId, RoomRequest roomRequest);
    ApiResponse deleteRoom(UUID hotelId, UUID roomId);
}

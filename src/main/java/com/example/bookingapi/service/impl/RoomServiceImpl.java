package com.example.bookingapi.service.impl;

import com.example.bookingapi.exception.ResourceNotFoundException;
import com.example.bookingapi.model.Hotel;
import com.example.bookingapi.model.Room;
import com.example.bookingapi.payload.request.RoomRequest;
import com.example.bookingapi.payload.response.ApiResponse;
import com.example.bookingapi.payload.response.RoomResponse;
import com.example.bookingapi.repository.HotelRepository;
import com.example.bookingapi.repository.RoomRepository;
import com.example.bookingapi.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RoomServiceImpl implements RoomService {

    @Autowired private RoomRepository roomRepository;
    @Autowired private HotelRepository hotelRepository;

    @Override
    public List<RoomResponse> getRoomsByHotel(UUID hotelId) {
        return roomRepository.findByHotelId(hotelId).stream()
                .map(room -> toRoomResponse(room, hotelId))
                .collect(Collectors.toList());
    }

    @Override
    public RoomResponse getRoom(UUID hotelId, UUID roomId) {
        Room room = roomRepository.findByIdAndHotelId(roomId, hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));
        return toRoomResponse(room, hotelId);
    }

    @Override
    public RoomResponse addRoom(UUID hotelId, RoomRequest roomRequest) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", hotelId));
        Room room = new Room();
        room.setHotel(hotel);
        room.setRoomNumber(roomRequest.getRoomNumber());
        room.setRoomType(roomRequest.getRoomType());
        room.setCapacity(roomRequest.getCapacity());
        room.setPricePerNight(roomRequest.getPricePerNight());
        return toRoomResponse(roomRepository.save(room), hotelId);
    }

    @Override
    public RoomResponse updateRoom(UUID hotelId, UUID roomId, RoomRequest roomRequest) {
        Room room = roomRepository.findByIdAndHotelId(roomId, hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));
        room.setRoomNumber(roomRequest.getRoomNumber());
        room.setRoomType(roomRequest.getRoomType());
        room.setCapacity(roomRequest.getCapacity());
        room.setPricePerNight(roomRequest.getPricePerNight());
        return toRoomResponse(roomRepository.save(room), hotelId);
    }

    private RoomResponse toRoomResponse(Room room, UUID hotelId) {
        return new RoomResponse(
                room.getId(),
                hotelId,
                room.getRoomNumber(),
                room.getRoomType(),
                room.getCapacity(),
                room.getPricePerNight()
        );
    }

    @Override
    public ApiResponse deleteRoom(UUID hotelId, UUID roomId) {
        Room room = roomRepository.findByIdAndHotelId(roomId, hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));
        roomRepository.delete(room);
        return new ApiResponse(true, "Room deleted successfully");
    }
}

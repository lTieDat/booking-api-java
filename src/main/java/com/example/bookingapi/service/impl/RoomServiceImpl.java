package com.example.bookingapi.service.impl;

import com.example.bookingapi.exception.ResourceNotFoundException;
import com.example.bookingapi.model.Hotel;
import com.example.bookingapi.model.Room;
import com.example.bookingapi.payload.request.RoomRequest;
import com.example.bookingapi.payload.response.ApiResponse;
import com.example.bookingapi.repository.HotelRepository;
import com.example.bookingapi.repository.RoomRepository;
import com.example.bookingapi.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RoomServiceImpl implements RoomService {

    @Autowired private RoomRepository roomRepository;
    @Autowired private HotelRepository hotelRepository;

    @Override
    public List<Room> getRoomsByHotel(UUID hotelId) {
        return roomRepository.findByHotelId(hotelId);
    }

    @Override
    public Room getRoom(UUID hotelId, UUID roomId) {
        return roomRepository.findByIdAndHotelId(roomId, hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));
    }

    @Override
    public Room addRoom(UUID hotelId, RoomRequest roomRequest) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", hotelId));
        Room room = new Room();
        room.setHotel(hotel);
        room.setRoomNumber(roomRequest.getRoomNumber());
        room.setRoomType(roomRequest.getRoomType());
        room.setCapacity(roomRequest.getCapacity());
        room.setPricePerNight(roomRequest.getPricePerNight());
        return roomRepository.save(room);
    }

    @Override
    public Room updateRoom(UUID hotelId, UUID roomId, RoomRequest roomRequest) {
        Room room = roomRepository.findByIdAndHotelId(roomId, hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));
        room.setRoomNumber(roomRequest.getRoomNumber());
        room.setRoomType(roomRequest.getRoomType());
        room.setCapacity(roomRequest.getCapacity());
        room.setPricePerNight(roomRequest.getPricePerNight());
        return roomRepository.save(room);
    }

    @Override
    public ApiResponse deleteRoom(UUID hotelId, UUID roomId) {
        Room room = roomRepository.findByIdAndHotelId(roomId, hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));
        roomRepository.delete(room);
        return new ApiResponse(true, "Room deleted successfully");
    }
}

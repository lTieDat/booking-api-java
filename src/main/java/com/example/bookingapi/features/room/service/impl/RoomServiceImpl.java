package com.example.bookingapi.features.room.service.impl;

import com.example.bookingapi.common.exception.ResourceNotFoundException;
import com.example.bookingapi.features.room.model.Room;
import com.example.bookingapi.features.room.model.RoomType;
import com.example.bookingapi.features.room.model.enums.RoomStatus;
import com.example.bookingapi.features.room.dto.request.RoomRequest;
import com.example.bookingapi.common.response.ApiMessageResponse;
import com.example.bookingapi.features.room.dto.response.RoomResponse;
import com.example.bookingapi.features.room.repository.RoomRepository;
import com.example.bookingapi.features.room.repository.RoomTypeRepository;
import com.example.bookingapi.features.room.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RoomServiceImpl implements RoomService {

    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomTypeRepository roomTypeRepository;

    @Override
    public List<RoomResponse> getRoomsByHotel(UUID hotelId) {
        return roomRepository.findByRoomType_Hotel_Id(hotelId).stream()
                .map(this::toRoomResponse)
                .collect(Collectors.toList());
    }

    @Override
    public RoomResponse getRoom(UUID hotelId, UUID roomId) {
        Room room = roomRepository.findByIdAndRoomType_Hotel_Id(roomId, hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));
        return toRoomResponse(room);
    }

    @Override
    public RoomResponse addRoom(UUID hotelId, RoomRequest roomRequest) {
        RoomType roomType = findRoomType(hotelId, roomRequest.getRoomTypeId());
        Room room = new Room();
        room.setRoomType(roomType);
        room.setRoomNumber(roomRequest.getRoomNumber());
        room.setFloor(roomRequest.getFloor());
        room.setStatus(roomRequest.getStatus() == null ? RoomStatus.AVAILABLE : roomRequest.getStatus());
        room.setIsActive(roomRequest.getIsActive() == null || roomRequest.getIsActive());
        return toRoomResponse(roomRepository.save(room));
    }

    @Override
    public RoomResponse updateRoom(UUID hotelId, UUID roomId, RoomRequest roomRequest) {
        Room room = roomRepository.findByIdAndRoomType_Hotel_Id(roomId, hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));
        RoomType roomType = findRoomType(hotelId, roomRequest.getRoomTypeId());
        room.setRoomNumber(roomRequest.getRoomNumber());
        room.setRoomType(roomType);
        room.setFloor(roomRequest.getFloor());
        room.setStatus(roomRequest.getStatus());
        room.setIsActive(roomRequest.getIsActive() == null || roomRequest.getIsActive());
        return toRoomResponse(roomRepository.save(room));
    }

    private RoomResponse toRoomResponse(Room room) {
        RoomType roomType = room.getRoomType();
        return new RoomResponse(
                room.getId(),
                roomType.getHotel().getId(),
                roomType.getId(),
                roomType.getName(),
                roomType.getCode(),
                room.getRoomNumber(),
                room.getFloor(),
                room.getStatus(),
                room.getIsActive()
        );
    }

    @Override
    public ApiMessageResponse deleteRoom(UUID hotelId, UUID roomId) {
        Room room = roomRepository.findByIdAndRoomType_Hotel_Id(roomId, hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));
        roomRepository.delete(room);
        return new ApiMessageResponse(true, "Room deleted successfully");
    }

    private RoomType findRoomType(UUID hotelId, UUID roomTypeId) {
        return roomTypeRepository.findByIdAndHotelId(roomTypeId, hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("RoomType", "id", roomTypeId));
    }
}

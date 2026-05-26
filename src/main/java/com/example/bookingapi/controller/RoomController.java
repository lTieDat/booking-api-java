package com.example.bookingapi.controller;

import com.example.bookingapi.annotation.CommonApiResponses;
import com.example.bookingapi.payload.request.RoomRequest;
import com.example.bookingapi.payload.response.RoomResponse;
import com.example.bookingapi.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hotels/{hotelId}/rooms")
@Tag(name = "Rooms", description = "Room listing and admin CRUD endpoints")
public class RoomController {

    @Autowired
    private RoomService roomService;

    @GetMapping
    @Operation(summary = "Get rooms by hotel", description = "Return all rooms for a hotel.")
    @ApiResponse(responseCode = "200", description = "Rooms returned successfully.",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = RoomResponse.class))))
    @CommonApiResponses
    public ResponseEntity<List<RoomResponse>> getRooms(@PathVariable UUID hotelId) {
        return ResponseEntity.ok(roomService.getRoomsByHotel(hotelId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get room by id", description = "Return a single room by hotel and room id.")
    @ApiResponse(responseCode = "200", description = "Room returned successfully.",
            content = @Content(schema = @Schema(implementation = RoomResponse.class)))
    @CommonApiResponses
    public ResponseEntity<RoomResponse> getRoom(@PathVariable UUID hotelId, @PathVariable UUID id) {
        return ResponseEntity.ok(roomService.getRoom(hotelId, id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create room", description = "Create a new room for a hotel. Admin only.")
    @ApiResponse(responseCode = "201", description = "Room created successfully.",
            content = @Content(schema = @Schema(implementation = RoomResponse.class)))
    @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden")
    @CommonApiResponses
    public ResponseEntity<RoomResponse> addRoom(@PathVariable UUID hotelId,
                                        @Valid @RequestBody RoomRequest roomRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.addRoom(hotelId, roomRequest));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update room", description = "Update a room. Admin only.")
    @ApiResponse(responseCode = "200", description = "Room updated successfully.",
            content = @Content(schema = @Schema(implementation = RoomResponse.class)))
    @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden")
    @CommonApiResponses
    public ResponseEntity<RoomResponse> updateRoom(@PathVariable UUID hotelId, @PathVariable UUID id,
                                           @Valid @RequestBody RoomRequest roomRequest) {
        return ResponseEntity.ok(roomService.updateRoom(hotelId, id, roomRequest));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete room", description = "Delete a room. Admin only.")
    @ApiResponse(responseCode = "200", description = "Room deleted successfully.",
            content = @Content(schema = @Schema(implementation = com.example.bookingapi.payload.response.ApiResponse.class)))
    @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden")
    @CommonApiResponses
    public ResponseEntity<com.example.bookingapi.payload.response.ApiResponse> deleteRoom(@PathVariable UUID hotelId, @PathVariable UUID id) {
        return ResponseEntity.ok(roomService.deleteRoom(hotelId, id));
    }
}

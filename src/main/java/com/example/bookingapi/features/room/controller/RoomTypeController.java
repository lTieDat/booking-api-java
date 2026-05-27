package com.example.bookingapi.features.room.controller;

import com.example.bookingapi.common.openapi.CommonApiResponses;
import com.example.bookingapi.features.room.dto.request.AmenityRequest;
import com.example.bookingapi.features.room.dto.request.RoomTypeRequest;
import com.example.bookingapi.features.room.dto.response.AmenityResponse;
import com.example.bookingapi.common.response.ApiMessageResponse;
import com.example.bookingapi.features.room.dto.response.RoomTypeResponse;
import com.example.bookingapi.features.room.service.RoomTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hotels/{hotelId}/room-types")
@Tag(name = "Room Types", description = "Room type listing, admin CRUD, and room type amenity endpoints")
public class RoomTypeController {

    @Autowired
    private RoomTypeService roomTypeService;

    @GetMapping
    @Operation(summary = "Get room types by hotel", description = "Return all room types for a hotel.")
    @ApiResponse(responseCode = "200", description = "Room types returned successfully.",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = RoomTypeResponse.class))))
    @CommonApiResponses
    public ResponseEntity<List<RoomTypeResponse>> getRoomTypes(@PathVariable UUID hotelId) {
        return ResponseEntity.ok(roomTypeService.getRoomTypesByHotel(hotelId));
    }

    @GetMapping("/{roomTypeId}")
    @Operation(summary = "Get room type by id", description = "Return a single room type by hotel and room type id.")
    @ApiResponse(responseCode = "200", description = "Room type returned successfully.",
            content = @Content(schema = @Schema(implementation = RoomTypeResponse.class)))
    @CommonApiResponses
    public ResponseEntity<RoomTypeResponse> getRoomType(
            @PathVariable UUID hotelId,
            @PathVariable UUID roomTypeId) {
        return ResponseEntity.ok(roomTypeService.getRoomType(hotelId, roomTypeId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create room type", description = "Create a new room type for a hotel. Admin only.")
    @ApiResponse(responseCode = "201", description = "Room type created successfully.",
            content = @Content(schema = @Schema(implementation = RoomTypeResponse.class)))
    @CommonApiResponses
    public ResponseEntity<RoomTypeResponse> addRoomType(
            @PathVariable UUID hotelId,
            @Valid @RequestBody RoomTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomTypeService.addRoomType(hotelId, request));
    }

    @PutMapping("/{roomTypeId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update room type", description = "Update a room type. Admin only.")
    @ApiResponse(responseCode = "200", description = "Room type updated successfully.",
            content = @Content(schema = @Schema(implementation = RoomTypeResponse.class)))
    @CommonApiResponses
    public ResponseEntity<RoomTypeResponse> updateRoomType(
            @PathVariable UUID hotelId,
            @PathVariable UUID roomTypeId,
            @Valid @RequestBody RoomTypeRequest request) {
        return ResponseEntity.ok(roomTypeService.updateRoomType(hotelId, roomTypeId, request));
    }

    @DeleteMapping("/{roomTypeId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete room type", description = "Delete a room type. Admin only.")
    @ApiResponse(responseCode = "200", description = "Room type deleted successfully.",
            content = @Content(schema = @Schema(implementation = ApiMessageResponse.class)))
    @CommonApiResponses
    public ResponseEntity<ApiMessageResponse> deleteRoomType(
            @PathVariable UUID hotelId,
            @PathVariable UUID roomTypeId) {
        return ResponseEntity.ok(roomTypeService.deleteRoomType(hotelId, roomTypeId));
    }

    @GetMapping("/{roomTypeId}/amenities")
    @Operation(summary = "Get room type amenities", description = "Return all amenities for a room type.")
    @ApiResponse(responseCode = "200", description = "Amenities returned successfully.",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = AmenityResponse.class))))
    @CommonApiResponses
    public ResponseEntity<List<AmenityResponse>> getAmenities(
            @PathVariable UUID hotelId,
            @PathVariable UUID roomTypeId) {
        return ResponseEntity.ok(roomTypeService.getAmenities(hotelId, roomTypeId));
    }

    @GetMapping("/{roomTypeId}/amenities/{amenityId}")
    @Operation(summary = "Get room type amenity", description = "Return a single amenity by room type and amenity id.")
    @ApiResponse(responseCode = "200", description = "Amenity returned successfully.",
            content = @Content(schema = @Schema(implementation = AmenityResponse.class)))
    @CommonApiResponses
    public ResponseEntity<AmenityResponse> getAmenity(
            @PathVariable UUID hotelId,
            @PathVariable UUID roomTypeId,
            @PathVariable UUID amenityId) {
        return ResponseEntity.ok(roomTypeService.getAmenity(hotelId, roomTypeId, amenityId));
    }

    @PostMapping("/{roomTypeId}/amenities")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Create room type amenity",
            description = "Create a new amenity for a room type. Admin only. Amenity code is generated by the backend from the name."
    )
    @ApiResponse(responseCode = "201", description = "Amenity created successfully.",
            content = @Content(schema = @Schema(implementation = AmenityResponse.class)))
    @CommonApiResponses
    public ResponseEntity<AmenityResponse> addAmenity(
            @PathVariable UUID hotelId,
            @PathVariable UUID roomTypeId,
            @Valid @RequestBody AmenityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roomTypeService.addAmenity(hotelId, roomTypeId, request));
    }

    @PutMapping("/{roomTypeId}/amenities/{amenityId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update room type amenity", description = "Update an amenity for a room type. Admin only.")
    @ApiResponse(responseCode = "200", description = "Amenity updated successfully.",
            content = @Content(schema = @Schema(implementation = AmenityResponse.class)))
    @CommonApiResponses
    public ResponseEntity<AmenityResponse> updateAmenity(
            @PathVariable UUID hotelId,
            @PathVariable UUID roomTypeId,
            @PathVariable UUID amenityId,
            @Valid @RequestBody AmenityRequest request) {
        return ResponseEntity.ok(roomTypeService.updateAmenity(hotelId, roomTypeId, amenityId, request));
    }

    @DeleteMapping("/{roomTypeId}/amenities/{amenityId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete room type amenity", description = "Delete an amenity from a room type. Admin only.")
    @ApiResponse(responseCode = "200", description = "Amenity deleted successfully.",
            content = @Content(schema = @Schema(implementation = ApiMessageResponse.class)))
    @CommonApiResponses
    public ResponseEntity<ApiMessageResponse> deleteAmenity(
            @PathVariable UUID hotelId,
            @PathVariable UUID roomTypeId,
            @PathVariable UUID amenityId) {
        return ResponseEntity.ok(roomTypeService.deleteAmenity(hotelId, roomTypeId, amenityId));
    }
}

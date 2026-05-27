package com.example.bookingapi.features.hotel.controller;

import com.example.bookingapi.common.openapi.CommonApiResponses;
import com.example.bookingapi.features.hotel.dto.request.HotelRequest;
import com.example.bookingapi.features.hotel.dto.response.HotelResponse;
import com.example.bookingapi.common.response.ApiMessageResponse;
import com.example.bookingapi.common.response.PagedResponse;
import com.example.bookingapi.features.hotel.service.HotelService;
import com.example.bookingapi.common.util.AppConstants;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.UUID;

@RestController
@RequestMapping("/api/hotels")
@Tag(name = "Hotels", description = "Hotel search and admin CRUD endpoints")
public class HotelController {

    @Autowired
    private HotelService hotelService;

    @GetMapping
    @Operation(summary = "Get all hotels", description = "Return paginated list of hotels.")
    @ApiResponse(responseCode = "200", description = "Hotels returned successfully.",
            content = @Content(schema = @Schema(implementation = PagedResponse.class)))
    @CommonApiResponses
    public ResponseEntity<PagedResponse<HotelResponse>> getAllHotels(
            @Parameter(description = "Page index starting from 0")
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @Parameter(description = "Hotel Name, Location or Landmark")
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(hotelService.getAllHotels(page, size, keyword));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get hotel by id", description = "Return a single hotel by its id.")
    @ApiResponse(responseCode = "200", description = "Hotel returned successfully.",
            content = @Content(schema = @Schema(implementation = HotelResponse.class)))
    @CommonApiResponses
    public ResponseEntity<HotelResponse> getHotel(@PathVariable UUID id) {
        return ResponseEntity.ok(hotelService.getHotel(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create hotel", description = "Create a new hotel. Admin only.")
    @ApiResponse(responseCode = "201", description = "Hotel created successfully.",
            content = @Content(schema = @Schema(implementation = HotelResponse.class)))
    @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden")
    @CommonApiResponses
    public ResponseEntity<HotelResponse> addHotel(@Valid @RequestBody HotelRequest hotelRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hotelService.addHotel(hotelRequest));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update hotel", description = "Update an existing hotel. Admin only.")
    @ApiResponse(responseCode = "200", description = "Hotel updated successfully.",
            content = @Content(schema = @Schema(implementation = HotelResponse.class)))
    @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden")
    @CommonApiResponses
    public ResponseEntity<HotelResponse> updateHotel(@PathVariable UUID id,
                                              @Valid @RequestBody HotelRequest hotelRequest) {
        return ResponseEntity.ok(hotelService.updateHotel(id, hotelRequest));
    }

    @PostMapping(value = "/{id}/preview-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Upload hotel preview image", description = "Upload a preview image to object storage and attach it to the hotel. Admin only.")
    @ApiResponse(responseCode = "200", description = "Hotel preview image uploaded successfully.",
            content = @Content(schema = @Schema(implementation = HotelResponse.class)))
    @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden")
    @CommonApiResponses
    public ResponseEntity<HotelResponse> uploadPreviewImage(
            @PathVariable UUID id,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String altText) {
        return ResponseEntity.ok(hotelService.uploadPreviewImage(id, file, altText));
    }

    @DeleteMapping("/{id}/preview-image")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete hotel preview image", description = "Delete the preview image metadata and object storage file when available. Admin only.")
    @ApiResponse(responseCode = "200", description = "Hotel preview image deleted successfully or already absent.",
            content = @Content(schema = @Schema(implementation = ApiMessageResponse.class)))
    @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden")
    @CommonApiResponses
    public ResponseEntity<ApiMessageResponse> deletePreviewImage(@PathVariable UUID id) {
        return ResponseEntity.ok(hotelService.deletePreviewImage(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete hotel", description = "Delete a hotel. Admin only.")
    @ApiResponse(responseCode = "200", description = "Hotel deleted successfully.",
            content = @Content(schema = @Schema(implementation = ApiMessageResponse.class)))
    @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden")
    @CommonApiResponses
    public ResponseEntity<ApiMessageResponse> deleteHotel(@PathVariable UUID id) {
        return ResponseEntity.ok(hotelService.deleteHotel(id));
    }
}

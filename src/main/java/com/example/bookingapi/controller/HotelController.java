package com.example.bookingapi.controller;

import com.example.bookingapi.payload.request.HotelRequest;
import com.example.bookingapi.payload.response.HotelResponse;
import com.example.bookingapi.payload.response.PagedResponse;
import com.example.bookingapi.service.HotelService;
import com.example.bookingapi.utils.AppConstants;
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
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hotels returned successfully.",
                    content = @Content(schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    public ResponseEntity<PagedResponse<HotelResponse>> getAllHotels(
            @Parameter(description = "Page index starting from 0")
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        return ResponseEntity.ok(hotelService.getAllHotels(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get hotel by id", description = "Return a single hotel by its id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hotel returned successfully.",
                    content = @Content(schema = @Schema(implementation = HotelResponse.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    public ResponseEntity<HotelResponse> getHotel(@PathVariable UUID id) {
        return ResponseEntity.ok(hotelService.getHotel(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create hotel", description = "Create a new hotel. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Hotel created successfully.",
                    content = @Content(schema = @Schema(implementation = HotelResponse.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    public ResponseEntity<HotelResponse> addHotel(@Valid @RequestBody HotelRequest hotelRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hotelService.addHotel(hotelRequest));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update hotel", description = "Update an existing hotel. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hotel updated successfully.",
                    content = @Content(schema = @Schema(implementation = HotelResponse.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    public ResponseEntity<HotelResponse> updateHotel(@PathVariable UUID id,
                                              @Valid @RequestBody HotelRequest hotelRequest) {
        return ResponseEntity.ok(hotelService.updateHotel(id, hotelRequest));
    }

    @PostMapping(value = "/{id}/preview-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Upload hotel preview image", description = "Upload a preview image to object storage and attach it to the hotel. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hotel preview image uploaded successfully.",
                    content = @Content(schema = @Schema(implementation = HotelResponse.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hotel preview image deleted successfully or already absent.",
                    content = @Content(schema = @Schema(implementation = com.example.bookingapi.payload.response.ApiResponse.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    public ResponseEntity<com.example.bookingapi.payload.response.ApiResponse> deletePreviewImage(@PathVariable UUID id) {
        return ResponseEntity.ok(hotelService.deletePreviewImage(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete hotel", description = "Delete a hotel. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hotel deleted successfully.",
                    content = @Content(schema = @Schema(implementation = com.example.bookingapi.payload.response.ApiResponse.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    public ResponseEntity<com.example.bookingapi.payload.response.ApiResponse> deleteHotel(@PathVariable UUID id) {
        return ResponseEntity.ok(hotelService.deleteHotel(id));
    }
}

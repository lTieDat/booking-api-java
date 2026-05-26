package com.example.bookingapi.service;

import com.example.bookingapi.payload.request.HotelRequest;
import com.example.bookingapi.payload.response.ApiResponse;
import com.example.bookingapi.payload.response.HotelResponse;
import com.example.bookingapi.payload.response.PagedResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface HotelService {
    PagedResponse<HotelResponse> getAllHotels(int page, int size, String keyword);
    HotelResponse getHotel(UUID id);
    HotelResponse addHotel(HotelRequest hotelRequest);
    HotelResponse updateHotel(UUID id, HotelRequest hotelRequest);
    HotelResponse uploadPreviewImage(UUID id, MultipartFile file, String altText);
    ApiResponse deletePreviewImage(UUID id);
    ApiResponse deleteHotel(UUID id);
}

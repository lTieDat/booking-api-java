package com.example.bookingapi.features.hotel.service;

import com.example.bookingapi.features.hotel.dto.request.HotelSearchRequest;
import com.example.bookingapi.features.hotel.dto.request.HotelRequest;
import com.example.bookingapi.common.response.ApiMessageResponse;
import com.example.bookingapi.common.security.UserPrincipal;
import com.example.bookingapi.features.hotel.dto.response.HotelResponse;
import com.example.bookingapi.features.hotel.dto.response.HotelSearchResponse;
import com.example.bookingapi.features.hotel.dto.response.ManagerHotelStatsResponse;
import com.example.bookingapi.common.response.PagedResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface HotelService {
    PagedResponse<HotelResponse> getAllHotels(int page, int size, String keyword);
    PagedResponse<HotelSearchResponse> searchHotels(HotelSearchRequest request);
    ManagerHotelStatsResponse getManagerHotelStats(UserPrincipal currentUser);
    HotelResponse getHotel(UUID id);
    HotelResponse addHotel(HotelRequest hotelRequest);
    HotelResponse updateHotel(UUID id, HotelRequest hotelRequest);
    HotelResponse uploadPreviewImage(UUID id, MultipartFile file, String altText);
    ApiMessageResponse deletePreviewImage(UUID id);
    ApiMessageResponse deleteHotel(UUID id);
}

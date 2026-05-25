package com.example.bookingapi.payload.response;

import com.example.bookingapi.model.enums.HotelImageType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class HotelImageResponse {
    private String url;
    private String bucket;
    private String objectKey;
    private String contentType;
    private Long size;
    private String altText;
    private HotelImageType imageType;
}

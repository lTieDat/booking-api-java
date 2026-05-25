package com.example.bookingapi.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HotelImageRequest {

    @NotBlank
    @Size(max = 500)
    private String url;

    @Size(max = 100)
    private String bucket;

    @Size(max = 500)
    private String objectKey;

    @Size(max = 100)
    private String contentType;

    private Long size;

    @NotBlank
    @Size(max = 100)
    private String altText;
}

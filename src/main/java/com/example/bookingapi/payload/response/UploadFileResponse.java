package com.example.bookingapi.payload.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UploadFileResponse {
    private String bucket;
    private String objectKey;
    private String url;
    private String contentType;
    private long size;
}

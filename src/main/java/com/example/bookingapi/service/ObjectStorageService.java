package com.example.bookingapi.service;

import com.example.bookingapi.payload.response.UploadFileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ObjectStorageService {
    UploadFileResponse upload(MultipartFile file, String folder);

    void delete(String bucket, String objectKey);
}

package com.example.bookingapi.common.storage;

import com.example.bookingapi.common.upload.UploadFileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ObjectStorageService {
    UploadFileResponse upload(MultipartFile file, String folder);

    void delete(String bucket, String objectKey);
}

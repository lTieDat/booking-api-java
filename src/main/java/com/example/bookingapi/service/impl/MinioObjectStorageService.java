package com.example.bookingapi.service.impl;

import com.example.bookingapi.config.MinioProperties;
import com.example.bookingapi.exception.AppException;
import com.example.bookingapi.exception.BadRequestException;
import com.example.bookingapi.payload.response.UploadFileResponse;
import com.example.bookingapi.service.ObjectStorageService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Service
public class MinioObjectStorageService implements ObjectStorageService {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioProperties minioProperties;

    @Override
    public UploadFileResponse upload(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File must not be empty");
        }

        String normalizedFolder = normalizeFolder(folder);
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String safeFilename = sanitizeFilename(originalFilename);
        String objectKey = buildObjectKey(normalizedFolder, safeFilename);
        String contentType = file.getContentType() != null ? file.getContentType() : DEFAULT_CONTENT_TYPE;

        try {
            ensureBucketReady();
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(minioProperties.getBucket())
                                .object(objectKey)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(contentType)
                                .build()
                );
            }

            return new UploadFileResponse(
                    minioProperties.getBucket(),
                    objectKey,
                    buildPublicUrl(objectKey),
                    contentType,
                    file.getSize()
            );
        } catch (Exception ex) {
            throw new AppException("Failed to upload file to object storage", ex);
        }
    }

    @Override
    public void delete(String bucket, String objectKey) {
        if (bucket == null || bucket.isBlank() || objectKey == null || objectKey.isBlank()) {
            throw new BadRequestException("Bucket and objectKey are required");
        }

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception ex) {
            throw new AppException("Failed to delete file from object storage", ex);
        }
    }

    private void ensureBucketReady() throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(minioProperties.getBucket()).build()
        );
        if (!exists) {
            if (!minioProperties.isAutoCreateBucket()) {
                throw new AppException("Configured MinIO bucket does not exist");
            }
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.getBucket()).build());
        }

        if (minioProperties.isPublicRead()) {
            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .config(publicReadPolicy(minioProperties.getBucket()))
                            .build()
            );
        }
    }

    private String buildObjectKey(String folder, String safeFilename) {
        LocalDate today = LocalDate.now();
        return folder + "/" + today.getYear() + "/" + String.format("%02d", today.getMonthValue()) + "/"
                + String.format("%02d", today.getDayOfMonth()) + "/" + UUID.randomUUID() + "-" + safeFilename;
    }

    private String normalizeFolder(String folder) {
        String value = (folder == null || folder.isBlank()) ? "hotel-images" : folder.trim();
        value = value.replace('\\', '/');
        value = value.replaceAll("^/+", "").replaceAll("/+$", "");
        return value.replaceAll("[^a-zA-Z0-9/_-]", "-").toLowerCase(Locale.ROOT);
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "-");
    }

    private String buildPublicUrl(String objectKey) {
        String base = minioProperties.getPublicEndpoint();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + minioProperties.getBucket() + "/" + objectKey;
    }

    private String publicReadPolicy(String bucket) {
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetBucketLocation", "s3:ListBucket"],
                      "Resource": ["arn:aws:s3:::%s"]
                    },
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucket, bucket);
    }
}

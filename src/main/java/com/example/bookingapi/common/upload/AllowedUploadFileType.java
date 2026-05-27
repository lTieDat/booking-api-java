package com.example.bookingapi.common.upload;

import java.util.Arrays;

public enum AllowedUploadFileType {
    JPEG("image/jpeg"),
    PNG("image/png"),
    WEBP("image/webp"),
    PDF("application/pdf"),
    DOC("application/msword"),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final String contentType;

    AllowedUploadFileType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    public static boolean isAllowed(String contentType) {
        return contentType != null && Arrays.stream(values())
                .anyMatch(type -> type.contentType.equals(contentType));
    }
}

package com.example.bookingapi.model;

import com.example.bookingapi.model.audit.DateAudit;
import com.example.bookingapi.model.enums.HotelImageType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "hotel_images")
@Getter
@Setter
@NoArgsConstructor
public class HotelImage extends DateAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @NotBlank
    @Size(max = 500)
    @Column(nullable = false, name = "url", length = 500)
    private String url;

    @Size(max = 100)
    @Column(name = "bucket", length = 100)
    private String bucket;

    @Size(max = 500)
    @Column(name = "object_key", length = 500)
    private String objectKey;

    @Size(max = 100)
    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, name = "alt_text", length = 100)
    private String altText;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false, length = 30)
    private HotelImageType imageType;
}

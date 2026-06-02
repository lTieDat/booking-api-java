package com.example.bookingapi.features.hotel.repository;

import com.example.bookingapi.features.hotel.model.Hotel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, UUID> {
    Page<Hotel> findByLocation_City(String city, Pageable pageable);

    @Query("""
            SELECT h
            FROM Hotel h
            JOIN h.location l
            WHERE LOWER(h.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(l.country) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(l.city) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(l.province) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(l.district) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(l.detail) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<Hotel> searchByNameOrLocation(String keyword, Pageable pageable);

    @Query(
            value = """
                    WITH requested_dates AS (
                        SELECT generate_series(CAST(:checkIn AS DATE), CAST(:checkOut AS DATE) - INTERVAL '1 day', INTERVAL '1 day')::date AS stay_date
                    ),
                    candidate_room_types AS (
                        SELECT
                            rt.id AS room_type_id,
                            rt.hotel_id AS hotel_id,
                            rt.base_price AS base_price
                        FROM room_types rt
                        JOIN hotels h ON h.id = rt.hotel_id
                        JOIN locations l ON l.id = h.location_id
                        WHERE rt.is_active = TRUE
                          AND rt.max_adults >= :adults
                          AND rt.max_children >= :children
                          AND rt.max_occupancy >= (:adults + :children)
                          AND l.latitude IS NOT NULL
                          AND l.longitude IS NOT NULL
                          AND (
                              :keyword IS NULL
                              OR TRIM(:keyword) = ''
                              OR LOWER(h.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                              OR LOWER(l.country) LIKE LOWER(CONCAT('%', :keyword, '%'))
                              OR LOWER(l.city) LIKE LOWER(CONCAT('%', :keyword, '%'))
                              OR LOWER(COALESCE(l.province, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                              OR LOWER(COALESCE(l.district, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                              OR LOWER(COALESCE(l.detail, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                          )
                    ),
                    available_room_types AS (
                        SELECT
                            crt.hotel_id,
                            crt.room_type_id,
                            crt.base_price
                        FROM candidate_room_types crt
                        WHERE NOT EXISTS (
                            SELECT 1
                            FROM requested_dates rd
                            WHERE (
                                (
                                    SELECT COUNT(*)
                                    FROM rooms room
                                    WHERE room.room_type_id = crt.room_type_id
                                      AND room.is_active = TRUE
                                      AND (room.status IS NULL OR room.status NOT IN ('MAINTENANCE', 'OUT_OF_SERVICE'))
                                ) - (
                                    SELECT COALESCE(SUM(br.quantity), 0)
                                    FROM booked_rooms br
                                    JOIN bookings b ON b.id = br.booking_id
                                    WHERE br.room_type_id = crt.room_type_id
                                      AND CAST(b.check_in_date_time AS DATE) <= rd.stay_date
                                      AND rd.stay_date < CAST(b.check_out_date_time AS DATE)
                                      AND b.status IN ('CONFIRMED', 'CHECKED_IN')
                                ) - (
                                    SELECT COALESCE(SUM(ih.quantity), 0)
                                    FROM inventory_holds ih
                                    WHERE ih.room_type_id = crt.room_type_id
                                      AND ih.date = rd.stay_date
                                      AND ih.status = 'ACTIVE'
                                )
                            ) < :roomCount
                        )
                    ),
                    rating_stats AS (
                        SELECT
                            rv.hotel_id,
                            CAST(AVG(rv.rating) AS NUMERIC(10, 2)) AS rating_avg,
                            COUNT(*) AS review_count
                        FROM reviews rv
                        WHERE rv.is_visible = TRUE
                        GROUP BY rv.hotel_id
                    ),
                    hotel_search_base AS (
                        SELECT
                            h.id AS hotel_id,
                            h.name AS hotel_name,
                            h.description AS hotel_description,
                            l.country,
                            l.city,
                            l.province,
                            l.district,
                            l.detail,
                            l.latitude,
                            l.longitude,
                            COALESCE(rs.rating_avg, 0) AS rating_avg,
                            COALESCE(rs.review_count, 0) AS review_count,
                            MIN(art.base_price) AS min_price,
                            CAST(
                                6371 * 2 * ASIN(
                                    SQRT(
                                        POWER(SIN(RADIANS(CAST(l.latitude AS DOUBLE PRECISION) - CAST(:latitude AS DOUBLE PRECISION)) / 2), 2)
                                        + COS(RADIANS(CAST(:latitude AS DOUBLE PRECISION)))
                                        * COS(RADIANS(CAST(l.latitude AS DOUBLE PRECISION)))
                                        * POWER(SIN(RADIANS(CAST(l.longitude AS DOUBLE PRECISION) - CAST(:longitude AS DOUBLE PRECISION)) / 2), 2)
                                    )
                                ) AS NUMERIC(10, 2)
                            ) AS distance_km
                        FROM available_room_types art
                        JOIN hotels h ON h.id = art.hotel_id
                        JOIN locations l ON l.id = h.location_id
                        LEFT JOIN rating_stats rs ON rs.hotel_id = h.id
                        GROUP BY
                            h.id,
                            h.name,
                            h.description,
                            l.country,
                            l.city,
                            l.province,
                            l.district,
                            l.detail,
                            l.latitude,
                            l.longitude,
                            rs.rating_avg,
                            rs.review_count
                    ),
                    filtered_hotels AS (
                        SELECT *
                        FROM hotel_search_base
                        WHERE :radiusKm IS NULL OR distance_km <= :radiusKm
                    )
                    SELECT
                        fh.hotel_id AS "id",
                        fh.hotel_name AS "name",
                        fh.hotel_description AS "description",
                        fh.country AS "country",
                        fh.city AS "city",
                        fh.province AS "province",
                        fh.district AS "district",
                        fh.detail AS "detail",
                        fh.latitude AS "latitude",
                        fh.longitude AS "longitude",
                        preview.url AS "previewImageUrl",
                        preview.bucket AS "previewImageBucket",
                        preview.object_key AS "previewImageObjectKey",
                        preview.content_type AS "previewImageContentType",
                        preview.size_bytes AS "previewImageSizeBytes",
                        preview.alt_text AS "previewImageAltText",
                        fh.distance_km AS "distanceKm",
                        fh.rating_avg AS "ratingAvg",
                        fh.review_count AS "reviewCount",
                        fh.min_price AS "minPrice"
                    FROM filtered_hotels fh
                    LEFT JOIN LATERAL (
                        SELECT
                            hi.url,
                            hi.bucket,
                            hi.object_key,
                            hi.content_type,
                            hi.size_bytes,
                            hi.alt_text
                        FROM hotel_images hi
                        WHERE hi.hotel_id = fh.hotel_id
                          AND hi.image_type = 'PREVIEW'
                        ORDER BY hi.created_at DESC
                        LIMIT 1
                    ) preview ON TRUE
                    ORDER BY
                        fh.rating_avg DESC,
                        fh.review_count DESC,
                        fh.distance_km ASC,
                        fh.min_price ASC NULLS LAST,
                        fh.hotel_name ASC
                    """,
            countQuery = """
                    WITH requested_dates AS (
                        SELECT generate_series(CAST(:checkIn AS DATE), CAST(:checkOut AS DATE) - INTERVAL '1 day', INTERVAL '1 day')::date AS stay_date
                    ),
                    candidate_room_types AS (
                        SELECT
                            rt.id AS room_type_id,
                            rt.hotel_id AS hotel_id
                        FROM room_types rt
                        JOIN hotels h ON h.id = rt.hotel_id
                        JOIN locations l ON l.id = h.location_id
                        WHERE rt.is_active = TRUE
                          AND rt.max_adults >= :adults
                          AND rt.max_children >= :children
                          AND rt.max_occupancy >= (:adults + :children)
                          AND l.latitude IS NOT NULL
                          AND l.longitude IS NOT NULL
                          AND (
                              :keyword IS NULL
                              OR TRIM(:keyword) = ''
                              OR LOWER(h.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                              OR LOWER(l.country) LIKE LOWER(CONCAT('%', :keyword, '%'))
                              OR LOWER(l.city) LIKE LOWER(CONCAT('%', :keyword, '%'))
                              OR LOWER(COALESCE(l.province, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                              OR LOWER(COALESCE(l.district, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                              OR LOWER(COALESCE(l.detail, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                          )
                    ),
                    available_room_types AS (
                        SELECT
                            crt.hotel_id,
                            crt.room_type_id
                        FROM candidate_room_types crt
                        WHERE NOT EXISTS (
                            SELECT 1
                            FROM requested_dates rd
                            WHERE (
                                (
                                    SELECT COUNT(*)
                                    FROM rooms room
                                    WHERE room.room_type_id = crt.room_type_id
                                      AND room.is_active = TRUE
                                      AND (room.status IS NULL OR room.status NOT IN ('MAINTENANCE', 'OUT_OF_SERVICE'))
                                ) - (
                                    SELECT COALESCE(SUM(br.quantity), 0)
                                    FROM booked_rooms br
                                    JOIN bookings b ON b.id = br.booking_id
                                    WHERE br.room_type_id = crt.room_type_id
                                      AND CAST(b.check_in_date_time AS DATE) <= rd.stay_date
                                      AND rd.stay_date < CAST(b.check_out_date_time AS DATE)
                                      AND b.status IN ('CONFIRMED', 'CHECKED_IN')
                                ) - (
                                    SELECT COALESCE(SUM(ih.quantity), 0)
                                    FROM inventory_holds ih
                                    WHERE ih.room_type_id = crt.room_type_id
                                      AND ih.date = rd.stay_date
                                      AND ih.status = 'ACTIVE'
                                )
                            ) < :roomCount
                        )
                    ),
                    hotel_search_base AS (
                        SELECT
                            h.id AS hotel_id,
                            CAST(
                                6371 * 2 * ASIN(
                                    SQRT(
                                        POWER(SIN(RADIANS(CAST(l.latitude AS DOUBLE PRECISION) - CAST(:latitude AS DOUBLE PRECISION)) / 2), 2)
                                        + COS(RADIANS(CAST(:latitude AS DOUBLE PRECISION)))
                                        * COS(RADIANS(CAST(l.latitude AS DOUBLE PRECISION)))
                                        * POWER(SIN(RADIANS(CAST(l.longitude AS DOUBLE PRECISION) - CAST(:longitude AS DOUBLE PRECISION)) / 2), 2)
                                    )
                                ) AS NUMERIC(10, 2)
                            ) AS distance_km
                        FROM available_room_types art
                        JOIN hotels h ON h.id = art.hotel_id
                        JOIN locations l ON l.id = h.location_id
                        GROUP BY h.id, l.latitude, l.longitude
                    )
                    SELECT COUNT(*)
                    FROM hotel_search_base
                    WHERE :radiusKm IS NULL OR distance_km <= :radiusKm
                    """,
            nativeQuery = true
    )
    Page<HotelSearchProjection> searchAvailableHotels(
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude,
            @Param("radiusKm") BigDecimal radiusKm,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("adults") int adults,
            @Param("children") int children,
            @Param("roomCount") int roomCount,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}

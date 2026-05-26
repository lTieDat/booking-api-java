package com.example.bookingapi.repository;

import com.example.bookingapi.model.Hotel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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
}

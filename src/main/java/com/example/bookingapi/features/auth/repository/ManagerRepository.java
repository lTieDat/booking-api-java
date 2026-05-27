package com.example.bookingapi.features.auth.repository;

import com.example.bookingapi.features.auth.model.Manager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ManagerRepository extends JpaRepository<Manager, UUID> {
    Optional<Manager> findByEmail(String email);
    Boolean existsByEmail(String email);
}

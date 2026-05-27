package com.example.bookingapi.features.auth.repository;

import com.example.bookingapi.features.auth.model.Role;
import com.example.bookingapi.features.auth.model.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByName(RoleName name);
}

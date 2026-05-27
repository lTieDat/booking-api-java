package com.example.bookingapi.common.security;

import com.example.bookingapi.features.auth.model.enums.ActorType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.UUID;

public interface CustomUserDetailsService extends UserDetailsService {
    UserDetails loadUserById(UUID id);
    UserDetails loadUserById(UUID id, ActorType actorType);
}

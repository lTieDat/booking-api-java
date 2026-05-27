package com.example.bookingapi.features.auth.service.impl;

import com.example.bookingapi.common.exception.ResourceNotFoundException;
import com.example.bookingapi.features.auth.model.Manager;
import com.example.bookingapi.features.user.model.User;
import com.example.bookingapi.features.auth.model.enums.ActorType;
import com.example.bookingapi.features.auth.repository.ManagerRepository;
import com.example.bookingapi.features.user.repository.UserRepository;
import com.example.bookingapi.common.security.CustomUserDetailsService;
import com.example.bookingapi.common.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomUserDetailsServiceImpl implements CustomUserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ManagerRepository managerRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(@NonNull String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail).orElse(null);
        if (user != null) {
            return UserPrincipal.create(user);
        }

        Manager manager = managerRepository.findByEmail(usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User or manager not found with username or email: " + usernameOrEmail));
        return UserPrincipal.create(manager);
    }

    @Override
    @Transactional
    public UserDetails loadUserById(UUID id) {
        return loadUserById(id, ActorType.USER);
    }

    @Override
    @Transactional
    public UserDetails loadUserById(UUID id, ActorType actorType) {
        if (actorType == ActorType.MANAGER) {
            Manager manager = managerRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Manager", "id", id));
            return UserPrincipal.create(manager);
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return UserPrincipal.create(user);
    }
}

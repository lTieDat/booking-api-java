package com.example.bookingapi.features.user.service.impl;

import com.example.bookingapi.common.exception.ResourceNotFoundException;
import com.example.bookingapi.features.user.model.User;
import com.example.bookingapi.features.user.dto.response.UserIdentityAvailability;
import com.example.bookingapi.features.user.dto.response.UserProfile;
import com.example.bookingapi.features.user.dto.response.UserSummary;
import com.example.bookingapi.features.user.repository.UserRepository;
import com.example.bookingapi.common.security.UserPrincipal;
import com.example.bookingapi.features.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserSummary getCurrentUser(UserPrincipal currentUser) {
        return new UserSummary(currentUser.getId(), currentUser.getUsername(), currentUser.getName());
    }

    @Override
    public UserProfile getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return new UserProfile(user.getId(), user.getUsername(), user.getName(), user.getCreatedAt());
    }

    @Override
    public UserIdentityAvailability checkUsernameAvailability(String username) {
        return new UserIdentityAvailability(!userRepository.existsByUsername(username));
    }

    @Override
    public UserIdentityAvailability checkEmailAvailability(String email) {
        return new UserIdentityAvailability(!userRepository.existsByEmail(email));
    }
}

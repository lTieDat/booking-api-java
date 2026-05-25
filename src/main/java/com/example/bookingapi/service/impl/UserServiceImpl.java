package com.example.bookingapi.service.impl;

import com.example.bookingapi.exception.ResourceNotFoundException;
import com.example.bookingapi.model.User;
import com.example.bookingapi.payload.response.UserIdentityAvailability;
import com.example.bookingapi.payload.response.UserProfile;
import com.example.bookingapi.payload.response.UserSummary;
import com.example.bookingapi.repository.UserRepository;
import com.example.bookingapi.security.UserPrincipal;
import com.example.bookingapi.service.UserService;
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

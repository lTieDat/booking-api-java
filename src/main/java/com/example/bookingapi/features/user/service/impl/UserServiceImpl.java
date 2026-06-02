package com.example.bookingapi.features.user.service.impl;

import com.example.bookingapi.common.exception.BadRequestException;
import com.example.bookingapi.common.exception.ConflictException;
import com.example.bookingapi.common.exception.ResourceNotFoundException;
import com.example.bookingapi.features.auth.model.enums.ActorType;
import com.example.bookingapi.features.user.dto.request.UpdateUserProfileRequest;
import com.example.bookingapi.features.user.dto.response.CurrentUserProfile;
import com.example.bookingapi.features.user.model.User;
import com.example.bookingapi.features.user.dto.response.UserIdentityAvailability;
import com.example.bookingapi.features.user.dto.response.UserProfile;
import com.example.bookingapi.features.user.dto.response.UserSummary;
import com.example.bookingapi.features.user.repository.UserRepository;
import com.example.bookingapi.common.security.UserPrincipal;
import com.example.bookingapi.features.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserSummary getCurrentUser(UserPrincipal currentUser) {
        return new UserSummary(currentUser.getId(), currentUser.getUsername(), currentUser.getName());
    }

    @Override
    public CurrentUserProfile getMyProfile(UserPrincipal currentUser) {
        return toCurrentUserProfile(findCurrentUser(currentUser));
    }

    @Override
    @Transactional
    public CurrentUserProfile updateMyProfile(UserPrincipal currentUser, UpdateUserProfileRequest request) {
        User user = findCurrentUser(currentUser);

        String username = request.getUsername().trim();
        String email = request.getEmail().trim();
        String name = request.getName().trim();

        if (userRepository.existsByUsernameAndIdNot(username, user.getId())) {
            throw new ConflictException("Username is already taken");
        }
        if (userRepository.existsByEmailAndIdNot(email, user.getId())) {
            throw new ConflictException("Email is already taken");
        }

        user.setUsername(username);
        user.setEmail(email);
        user.setName(name);
        return toCurrentUserProfile(userRepository.save(user));
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

    private User findCurrentUser(UserPrincipal currentUser) {
        if (currentUser.getActorType() != ActorType.USER) {
            throw new BadRequestException("Only user accounts can access this profile");
        }
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));
    }

    private CurrentUserProfile toCurrentUserProfile(User user) {
        return new CurrentUserProfile(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getEmail(),
                user.getIsVerified(),
                user.getCreatedAt()
        );
    }
}

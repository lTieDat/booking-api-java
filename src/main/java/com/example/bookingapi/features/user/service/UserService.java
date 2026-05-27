package com.example.bookingapi.features.user.service;

import com.example.bookingapi.features.user.dto.response.UserIdentityAvailability;
import com.example.bookingapi.features.user.dto.response.UserProfile;
import com.example.bookingapi.features.user.dto.response.UserSummary;
import com.example.bookingapi.common.security.UserPrincipal;

public interface      UserService {
    UserSummary getCurrentUser(UserPrincipal currentUser);
    UserProfile getUserProfile(String username);
    UserIdentityAvailability checkUsernameAvailability(String username);
    UserIdentityAvailability checkEmailAvailability(String email);
}

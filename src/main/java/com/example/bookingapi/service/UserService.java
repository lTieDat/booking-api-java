package com.example.bookingapi.service;

import com.example.bookingapi.payload.response.UserIdentityAvailability;
import com.example.bookingapi.payload.response.UserProfile;
import com.example.bookingapi.payload.response.UserSummary;
import com.example.bookingapi.security.UserPrincipal;

public interface UserService {
    UserSummary getCurrentUser(UserPrincipal currentUser);
    UserProfile getUserProfile(String username);
    UserIdentityAvailability checkUsernameAvailability(String username);
    UserIdentityAvailability checkEmailAvailability(String email);
}

package com.example.bookingapi.tests.user.service;

import com.example.bookingapi.common.exception.ConflictException;
import com.example.bookingapi.common.security.UserPrincipal;
import com.example.bookingapi.features.auth.model.enums.ActorType;
import com.example.bookingapi.features.user.dto.request.UpdateUserProfileRequest;
import com.example.bookingapi.features.user.dto.response.CurrentUserProfile;
import com.example.bookingapi.features.user.model.User;
import com.example.bookingapi.features.user.repository.UserRepository;
import com.example.bookingapi.features.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private UserServiceImpl userService;

    @Test
    void updateMyProfileRejectsDuplicatedUsername() {
        User user = buildUser();
        UserPrincipal currentUser = buildPrincipal(user.getId());
        UpdateUserProfileRequest request = buildRequest();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameAndIdNot("newusername", user.getId())).thenReturn(true);

        assertThrows(ConflictException.class, () -> userService.updateMyProfile(currentUser, request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateMyProfilePersistsTrimmedFields() {
        User user = buildUser();
        UserPrincipal currentUser = buildPrincipal(user.getId());
        UpdateUserProfileRequest request = buildRequest();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameAndIdNot("newusername", user.getId())).thenReturn(false);
        when(userRepository.existsByEmailAndIdNot("new@example.com", user.getId())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CurrentUserProfile response = userService.updateMyProfile(currentUser, request);

        assertEquals("New Name", user.getName());
        assertEquals("newusername", user.getUsername());
        assertEquals("new@example.com", user.getEmail());
        assertEquals("newusername", response.getUsername());
        assertEquals("new@example.com", response.getEmail());
        assertEquals(Boolean.TRUE, response.getVerified());
        verify(userRepository).save(user);
    }

    private User buildUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Old Name");
        user.setUsername("oldusername");
        user.setEmail("old@example.com");
        user.setPassword("secret");
        user.setIsVerified(true);
        user.setRoles(Set.of());
        user.setCreatedAt(Instant.parse("2026-06-01T00:00:00Z"));
        return user;
    }

    private UserPrincipal buildPrincipal(UUID userId) {
        return new UserPrincipal(
                userId,
                "Old Name",
                "oldusername",
                "old@example.com",
                "secret",
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                ActorType.USER,
                true
        );
    }

    private UpdateUserProfileRequest buildRequest() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setName("  New Name  ");
        request.setUsername("  newusername  ");
        request.setEmail("  new@example.com  ");
        return request;
    }
}

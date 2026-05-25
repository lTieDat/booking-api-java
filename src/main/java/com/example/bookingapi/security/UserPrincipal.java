package com.example.bookingapi.security;

import com.example.bookingapi.model.Manager;
import com.example.bookingapi.model.User;
import com.example.bookingapi.model.enums.ActorType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private UUID id;
    private String name;
    private String username;

    @JsonIgnore
    private String email;

    @JsonIgnore
    private String password;

    private Collection<? extends GrantedAuthority> authorities;

    private ActorType actorType;

    private boolean enabled;

    public static UserPrincipal create(User user) {
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());
        return new UserPrincipal(
                user.getId(), user.getName(), user.getUsername(),
                user.getEmail(), user.getPassword(), authorities,
                ActorType.USER, Boolean.TRUE.equals(user.getIsVerified())
        );
    }

    public static UserPrincipal create(Manager manager) {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        return new UserPrincipal(
                manager.getId(), manager.getFullName(), manager.getEmail(),
                manager.getEmail(), manager.getPasswordHash(), authorities,
                ActorType.MANAGER, Boolean.TRUE.equals(manager.getIsActive())
        );
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return enabled; }
}

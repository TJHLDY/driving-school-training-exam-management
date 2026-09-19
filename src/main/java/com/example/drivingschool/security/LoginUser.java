package com.example.drivingschool.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public record LoginUser(Long userId, String username, String passwordHash,
                        Set<String> roleCodes, boolean enabled) implements UserDetails {
    @Serial
    private static final long serialVersionUID = 1L;

    public LoginUser {
        roleCodes = roleCodes == null ? Set.of() : new LinkedHashSet<>(roleCodes);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roleCodes.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}

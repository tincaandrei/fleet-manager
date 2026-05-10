package com.fleet.auth.entity;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public enum Role {
    USER,
    STAFF,
    ADMIN;

    public String asAuthority() {
        return "ROLE_" + name();
    }

    public static Role fromValue(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Role value must not be blank");
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }
        return Role.valueOf(normalized);
    }

    public static Set<Role> normalize(Collection<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return EnumSet.noneOf(Role.class);
        }

        EnumSet<Role> normalized = EnumSet.noneOf(Role.class);
        normalized.addAll(roles);
        return normalized;
    }

    public static Set<Role> fromAuthorities(Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null || authorities.isEmpty()) {
            return EnumSet.noneOf(Role.class);
        }

        EnumSet<Role> roles = EnumSet.noneOf(Role.class);
        for (GrantedAuthority authority : authorities) {
            roles.add(fromValue(authority.getAuthority()));
        }
        return roles;
    }

    public static Set<GrantedAuthority> toAuthorities(Collection<Role> roles) {
        return normalize(roles).stream()
                .map(Role::asAuthority)
                .map(SimpleGrantedAuthority::new)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public static Set<GrantedAuthority> toAuthorities(Role role) {
        return toAuthorities(Set.of(role));
    }
}

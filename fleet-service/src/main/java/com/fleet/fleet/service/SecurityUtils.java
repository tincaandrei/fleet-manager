package com.fleet.fleet.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;
import java.util.stream.Collectors;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static boolean canReadAll(Authentication authentication) {
        Set<String> roles = roles(authentication);
        return roles.contains("ADMIN") || roles.contains("FLEET_MANAGER") || roles.contains("AUDITOR");
    }

    public static boolean isEmployeeOnly(Authentication authentication) {
        Set<String> roles = roles(authentication);
        return !canReadAll(authentication) && (roles.contains("EMPLOYEE") || roles.contains("USER"));
    }

    public static Long currentUserId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof JwtPrincipal jwtPrincipal) {
            return jwtPrincipal.userId();
        }
        return null;
    }

    private static Set<String> roles(Authentication authentication) {
        if (authentication == null) {
            return Set.of();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.startsWith("ROLE_") ? authority.substring(5) : authority)
                .collect(Collectors.toUnmodifiableSet());
    }
}

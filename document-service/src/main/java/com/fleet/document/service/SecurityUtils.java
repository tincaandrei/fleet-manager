package com.fleet.document.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;
import java.util.stream.Collectors;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static boolean canReview(Authentication authentication) {
        Set<String> roles = roles(authentication);
        return roles.contains("SUPERADMIN") || roles.contains("BUSINESS_ADMIN");
    }

    public static boolean isSuperadmin(Authentication authentication) {
        return roles(authentication).contains("SUPERADMIN");
    }

    public static boolean isBusinessAdmin(Authentication authentication) {
        return roles(authentication).contains("BUSINESS_ADMIN");
    }

    public static boolean isEmployee(Authentication authentication) {
        return roles(authentication).contains("EMPLOYEE");
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

    public static Long currentBusinessId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof JwtPrincipal jwtPrincipal) {
            return jwtPrincipal.businessId();
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

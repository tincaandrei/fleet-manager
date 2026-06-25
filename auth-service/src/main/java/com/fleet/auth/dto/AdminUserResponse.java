package com.fleet.auth.dto;

import com.fleet.auth.entity.Role;
import com.fleet.auth.entity.UserStatus;

import java.util.Set;

public record AdminUserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        Set<Role> roles,
        UserStatus status,
        boolean enabled,
        boolean passwordChangeRequired,
        Long businessId
) {
}

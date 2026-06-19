package com.fleet.auth.dto;

import com.fleet.auth.entity.Role;
import jakarta.validation.constraints.NotNull;

public record AssignBusinessUserRequest(
        @NotNull(message = "Business id is required")
        Long businessId,
        @NotNull(message = "Role is required")
        Role role
) {
}

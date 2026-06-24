package com.fleet.auth.dto;

import com.fleet.auth.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UserLookupResponse", description = "Minimal user data for service-to-service display lookups.")
public record UserLookupResponse(
        Long userId,
        String username,
        String email,
        Long businessId,
        Role role
) {
}

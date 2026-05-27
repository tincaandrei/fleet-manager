package com.fleet.auth.dto;

import com.fleet.auth.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AuthResponse", description = "Authentication result with bearer token.")
public record AuthResponse(
        @Schema(description = "JWT bearer token to be sent as `Authorization: Bearer <token>`.", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbGljZSJ9.signature")
        String token,
        @Schema(description = "Authenticated username.", example = "alice")
        String username,
        @Schema(description = "Current role embedded in the token.", example = "USER")
        Role role,
        Long userId,
        Long businessId,
        String businessName
) {
}

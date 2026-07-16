package com.fleet.fleet.dto;

public record AuthUserLookupResponse(
        Long userId,
        String username,
        String email,
        Long businessId,
        String role,
        String status,
        Boolean enabled
) {
}

package com.fleet.document.dto;

public record UserLookupResponse(
        Long userId,
        String username,
        String email,
        Long businessId,
        String role
) {
}

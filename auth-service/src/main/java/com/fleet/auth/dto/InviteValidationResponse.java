package com.fleet.auth.dto;

public record InviteValidationResponse(
        boolean valid,
        String email,
        String message
) {
}

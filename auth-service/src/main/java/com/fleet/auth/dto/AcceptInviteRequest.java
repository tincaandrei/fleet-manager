package com.fleet.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AcceptInviteRequest(
        @NotBlank(message = "Invitation token is required")
        String token,
        @NotBlank(message = "New password is required")
        String newPassword
) {
}

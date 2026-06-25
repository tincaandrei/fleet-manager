package com.fleet.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Schema(name = "UpdateUserRequest", description = "Payload used to update user profile data.")
public record UpdateUserRequest(
        @Schema(description = "Display username. Used for profile display, not login.", example = "alice")
        @Size(max = 50, message = "Username must be at most 50 characters")
        String username,
        @Schema(description = "User email address.", example = "alice@example.com")
        @Email(message = "Email must be valid")
        String email,
        @Schema(description = "Optional phone number.", example = "+40123456789", nullable = true)
        @Size(max = 30, message = "Phone must be at most 30 characters")
        String phone,
        @Schema(description = "Optional address.", example = "Main Street 1", nullable = true)
        @Size(max = 255, message = "Address must be at most 255 characters")
        String address,
        @Schema(description = "Optional new password. Leave null/blank to keep the current password.", example = "password123")
        String password
) {
}

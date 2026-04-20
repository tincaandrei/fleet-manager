package com.fleet.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "RegisterRequest", description = "Payload used to create a new user account.")
public record RegisterRequest(
        @Schema(description = "Unique username used to sign in.", example = "alice", minLength = 3, maxLength = 50, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,
        @Schema(description = "User email address.", example = "alice@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,
        @Schema(description = "Raw password that will be hashed by the service.", example = "password123", minLength = 6, maxLength = 255, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 255, message = "Password must be between 6 and 255 characters")
        String password,
        @Schema(description = "Optional phone number.", example = "+40123456789", maxLength = 30, nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 30, message = "Phone must be at most 30 characters")
        String phone,
        @Schema(description = "Optional address.", example = "Main Street 1", maxLength = 255, nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 255, message = "Address must be at most 255 characters")
        String address
) {
}

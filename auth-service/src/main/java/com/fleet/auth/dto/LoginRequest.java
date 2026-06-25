package com.fleet.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import org.springframework.util.StringUtils;

@Schema(name = "LoginRequest", description = "Credentials used to obtain a JWT token.")
public record LoginRequest(
        @Schema(description = "Email of an existing account.", example = "alice@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @Email(message = "Email must be valid")
        String email,
        @Schema(description = "Legacy login identifier. Prefer email.", example = "alice@example.com")
        String username,
        @Schema(description = "Password of the account.", example = "password123", requiredMode = Schema.RequiredMode.REQUIRED)
        String password
) {
    @AssertTrue(message = "Email is required")
    public boolean hasLoginIdentifier() {
        return StringUtils.hasText(email) || StringUtils.hasText(username);
    }

    @AssertTrue(message = "Password is required")
    public boolean hasPassword() {
        return StringUtils.hasText(password);
    }

    public String loginEmail() {
        return StringUtils.hasText(email) ? email.trim() : username.trim();
    }
}

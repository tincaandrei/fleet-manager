package com.fleet.auth.dto;

import com.fleet.auth.entity.Role;
import com.fleet.auth.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MeResponse", description = "User profile response.")
public record MeResponse(
        @Schema(description = "Numeric user identifier.", example = "1")
        Long userId,
        @Schema(description = "Unique username.", example = "alice")
        String username,
        @Schema(description = "Email address.", example = "alice@example.com")
        String email,
        @Schema(description = "Optional phone number.", example = "+40123456789", nullable = true)
        String phone,
        @Schema(description = "Optional address.", example = "Main Street 1", nullable = true)
        String address,
        @Schema(description = "Current user role.", example = "USER")
        Role role,
        UserStatus status,
        Long businessId,
        String businessName,
        @Schema(description = "Authenticated endpoint for loading this user's profile image.", nullable = true)
        String profileImageUrl,
        @Schema(description = "Original profile image file name.", nullable = true)
        String profileImageOriginalFileName
) {
}

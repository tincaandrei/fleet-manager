package com.fleet.auth.dto;

import com.fleet.auth.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "UpdateRoleRequest", description = "Payload used by admins to update a user's role.")
public record UpdateRoleRequest(
        @Schema(description = "New role assigned to the target user.", example = "BUSINESS_ADMIN", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Role is required")
        Role role
) {
}

package com.fleet.fleet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload used to assign or reassign a vehicle.")
public record VehicleAssignmentRequest(
        @Schema(description = "Application user id assigned to the vehicle. Send null to clear it.", example = "12")
        Long assignedUserId,

        @Size(max = 160, message = "Assigned driver name must be at most 160 characters")
        @Schema(description = "Human-readable assigned driver name. Send null to clear it.", example = "Alex Ionescu")
        String assignedDriverName,

        @Size(max = 120, message = "Department must be at most 120 characters")
        @Schema(description = "Department responsible for the vehicle after assignment.", example = "Operations")
        String department
) {
}

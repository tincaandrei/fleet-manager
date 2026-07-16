package com.fleet.fleet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

@Schema(description = "Payload used to assign or reassign a vehicle.")
public record VehicleAssignmentRequest(
        @Schema(description = "Application user id assigned to the vehicle. Send null to clear it.", example = "12")
        @Positive(message = "Assigned user id must be positive")
        Long assignedUserId
) {
}

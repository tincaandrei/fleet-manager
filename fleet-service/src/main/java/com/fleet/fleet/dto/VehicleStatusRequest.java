package com.fleet.fleet.dto;

import com.fleet.fleet.entity.VehicleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload used to change a vehicle status.")
public record VehicleStatusRequest(
        @NotNull(message = "Status is required")
        @Schema(description = "New vehicle status.", example = "IN_SERVICE", requiredMode = Schema.RequiredMode.REQUIRED)
        VehicleStatus status
) {
}

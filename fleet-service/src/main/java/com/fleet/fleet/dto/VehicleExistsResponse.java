package com.fleet.fleet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Vehicle existence response for internal service-to-service lookups.")
public record VehicleExistsResponse(
        @Schema(description = "Vehicle id that was checked.", example = "1")
        Long id,
        @Schema(description = "Whether the vehicle exists.", example = "true")
        boolean exists
) {
}

package com.fleet.fleet.dto;

import com.fleet.fleet.entity.VehicleStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Reduced vehicle view for internal service-to-service lookups.")
public record VehicleBasicInfoResponse(
        @Schema(description = "Vehicle id.", example = "1")
        Long id,
        Long businessId,
        @Schema(description = "Unique vehicle license plate.", example = "B-123-ABC")
        String licensePlate,
        @Schema(description = "Vehicle brand or manufacturer.", example = "Toyota")
        String brand,
        @Schema(description = "Vehicle model.", example = "Corolla")
        String model,
        @Schema(description = "Current operational status.", example = "ACTIVE")
        VehicleStatus status,
        @Schema(description = "Application user id assigned to the vehicle.", example = "12")
        Long assignedUserId,
        @Schema(description = "Human-readable assigned driver name.", example = "Alex Ionescu")
        String assignedDriverName
) {
}

package com.fleet.fleet.dto;

import com.fleet.fleet.entity.FuelType;
import com.fleet.fleet.entity.OwnershipType;
import com.fleet.fleet.entity.VehicleStatus;
import com.fleet.fleet.entity.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Vehicle record returned by Fleet Service.")
public record VehicleResponse(
        @Schema(description = "Vehicle id.", example = "1")
        Long id,
        Long businessId,
        @Schema(description = "Unique vehicle license plate.", example = "B-123-ABC")
        String licensePlate,
        @Schema(description = "Vehicle identification number.", example = "1HGCM82633A004352")
        String vin,
        @Schema(description = "Vehicle brand or manufacturer.", example = "Toyota")
        String brand,
        @Schema(description = "Vehicle model.", example = "Corolla")
        String model,
        @Schema(description = "Manufacture year.", example = "2022")
        Integer manufactureYear,
        @Schema(description = "Operational vehicle category.", example = "CAR")
        VehicleType vehicleType,
        @Schema(description = "Fuel or powertrain type.", example = "HYBRID")
        FuelType fuelType,
        @Schema(description = "Ownership model used by the company.", example = "OWNED")
        OwnershipType ownershipType,
        @Schema(description = "Current operational status.", example = "ACTIVE")
        VehicleStatus status,
        @Schema(description = "Department currently responsible for the vehicle.", example = "Operations")
        String department,
        @Schema(description = "Application user id assigned to the vehicle.", example = "12")
        Long assignedUserId,
        @Schema(description = "Human-readable assigned driver name.", example = "Alex Ionescu")
        String assignedDriverName,
        @Schema(description = "Current odometer value.", example = "25000")
        Long currentMileage,
        @Schema(description = "Creation timestamp.", example = "2026-05-04T11:30:00Z")
        Instant createdAt,
        @Schema(description = "Last update timestamp.", example = "2026-05-04T11:45:00Z")
        Instant updatedAt
) {
}

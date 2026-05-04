package com.fleet.fleet.dto;

import com.fleet.fleet.entity.FuelType;
import com.fleet.fleet.entity.OwnershipType;
import com.fleet.fleet.entity.VehicleStatus;
import com.fleet.fleet.entity.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload used to create or replace a vehicle in the fleet registry.")
public record VehicleRequest(
        @NotBlank(message = "License plate is required")
        @Size(max = 32, message = "License plate must be at most 32 characters")
        @Schema(description = "Unique vehicle license plate.", example = "B-123-ABC", requiredMode = Schema.RequiredMode.REQUIRED)
        String licensePlate,

        @Size(max = 64, message = "VIN must be at most 64 characters")
        @Schema(description = "Vehicle identification number. Optional, but unique when provided.", example = "1HGCM82633A004352")
        String vin,

        @NotBlank(message = "Brand is required")
        @Size(max = 80, message = "Brand must be at most 80 characters")
        @Schema(description = "Vehicle brand or manufacturer.", example = "Toyota", requiredMode = Schema.RequiredMode.REQUIRED)
        String brand,

        @NotBlank(message = "Model is required")
        @Size(max = 80, message = "Model must be at most 80 characters")
        @Schema(description = "Vehicle model.", example = "Corolla", requiredMode = Schema.RequiredMode.REQUIRED)
        String model,

        @Min(value = 1886, message = "Manufacture year must be 1886 or later")
        @Schema(description = "Manufacture year.", example = "2022", minimum = "1886")
        Integer manufactureYear,

        @Schema(description = "Operational vehicle category.", example = "CAR")
        VehicleType vehicleType,

        @Schema(description = "Fuel or powertrain type.", example = "HYBRID")
        FuelType fuelType,

        @Schema(description = "Ownership model used by the company.", example = "OWNED")
        OwnershipType ownershipType,

        @Schema(description = "Current operational status. Defaults to ACTIVE when omitted on create.", example = "ACTIVE")
        VehicleStatus status,

        @Size(max = 120, message = "Department must be at most 120 characters")
        @Schema(description = "Department currently responsible for the vehicle.", example = "Operations")
        String department,

        @Schema(description = "Application user id assigned to the vehicle.", example = "12")
        Long assignedUserId,

        @Size(max = 160, message = "Assigned driver name must be at most 160 characters")
        @Schema(description = "Human-readable assigned driver name.", example = "Alex Ionescu")
        String assignedDriverName,

        @PositiveOrZero(message = "Current mileage must be zero or positive")
        @Schema(description = "Current odometer value.", example = "25000", minimum = "0")
        Long currentMileage
) {
}

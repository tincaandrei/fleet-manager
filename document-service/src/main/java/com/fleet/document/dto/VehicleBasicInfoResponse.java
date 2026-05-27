package com.fleet.document.dto;

public record VehicleBasicInfoResponse(
        Long id,
        Long businessId,
        String licensePlate,
        String brand,
        String model,
        String status,
        Long assignedUserId,
        String assignedDriverName
) {
}

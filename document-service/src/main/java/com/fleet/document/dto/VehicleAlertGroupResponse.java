package com.fleet.document.dto;

import java.util.List;

public record VehicleAlertGroupResponse(
        Long vehicleId,
        Long businessId,
        String licensePlate,
        String brand,
        String model,
        Long assignedUserId,
        String assignedDriverName,
        List<VehicleDocumentAttributeResponse> alerts
) {
}

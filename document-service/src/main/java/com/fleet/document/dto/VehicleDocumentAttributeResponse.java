package com.fleet.document.dto;

import com.fleet.document.entity.ApprovedDataStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Normalized vehicle document attribute used for expiration alerts.")
public record VehicleDocumentAttributeResponse(
        UUID id,
        Long vehicleId,
        Long businessId,
        UUID documentId,
        UUID approvedDataId,
        String documentType,
        String subtype,
        String licensePlate,
        String vin,
        LocalDate validFrom,
        LocalDate validUntil,
        Map<String, Object> sourceData,
        ApprovedDataStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}

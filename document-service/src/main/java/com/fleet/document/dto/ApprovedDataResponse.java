package com.fleet.document.dto;

import com.fleet.document.entity.ApprovedDataStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Reviewed and approved document data.")
public record ApprovedDataResponse(
        UUID id,
        Map<String, Object> approvedData,
        Long approvedByUserId,
        Instant approvedAt,
        String reviewComment,
        ApprovedDataStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}

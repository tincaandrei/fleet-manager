package com.fleet.document.dto;

import com.fleet.document.entity.DocumentStatus;
import com.fleet.document.entity.DocumentType;

import java.time.Instant;
import java.util.UUID;

public record DocumentHistoryResponse(
        UUID documentId,
        String originalFileName,
        String contentType,
        Long fileSize,
        Long vehicleId,
        Long businessId,
        DocumentType documentType,
        String documentSubtype,
        DocumentStatus status,
        Long uploadedByUserId,
        String uploadedByUsername,
        String uploadedByEmail,
        Instant uploadedAt,
        Instant updatedAt
) {
}

package com.fleet.document.dto;

import com.fleet.document.entity.DocumentStatus;
import com.fleet.document.entity.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Document metadata with role-dependent extraction and approved data sections.")
public record DocumentResponse(
        UUID id,
        Long vehicleId,
        DocumentType documentType,
        DocumentStatus status,
        String originalFileName,
        String storedFileName,
        String contentType,
        Long fileSize,
        String storagePath,
        Long uploadedByUserId,
        Instant createdAt,
        Instant updatedAt,
        ExtractionResponse extraction,
        ApprovedDataResponse approvedData
) {
}

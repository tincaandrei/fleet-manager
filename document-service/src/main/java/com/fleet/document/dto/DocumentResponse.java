package com.fleet.document.dto;

import com.fleet.document.entity.DocumentStatus;
import com.fleet.document.entity.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Document metadata with approved review data.")
public record DocumentResponse(
        UUID id,
        Long vehicleId,
        Long businessId,
        DocumentType documentType,
        String documentSubtype,
        DocumentStatus status,
        String originalFileName,
        String storedFileName,
        String contentType,
        Long fileSize,
        String storagePath,
        Long uploadedByUserId,
        Instant createdAt,
        Instant updatedAt,
        DocumentExtractionResponse extraction,
        ApprovedDocumentDataResponse approvedData
) {
}

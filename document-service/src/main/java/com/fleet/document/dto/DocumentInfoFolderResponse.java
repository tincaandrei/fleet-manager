package com.fleet.document.dto;

import com.fleet.document.entity.DocumentStatus;
import com.fleet.document.entity.DocumentType;

import java.util.Map;
import java.util.UUID;

public record DocumentInfoFolderResponse(
        UUID documentId,
        Long vehicleId,
        Long businessId,
        DocumentType documentType,
        String documentSubtype,
        DocumentStatus status,
        String originalFileName,
        Map<String, Object> canonicalFields,
        Map<String, Object> extraFields
) {
}

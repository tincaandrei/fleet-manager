package com.fleet.parser.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ParserExtractionRequest(
        @NotNull
        UUID documentId,
        @NotNull
        Long vehicleId,
        String declaredDocumentType,
        String originalFileName,
        String storedFileName,
        String contentType,
        Long fileSize,
        String storagePath
) {
}

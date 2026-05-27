package com.fleet.parser.dto;

public record DocumentTypeDetectionResult(
        DocumentType documentType,
        DocumentSubtype subtype
) {
}

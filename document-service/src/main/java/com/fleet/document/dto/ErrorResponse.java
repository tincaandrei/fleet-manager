package com.fleet.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error response returned for validation and business rule failures.")
public record ErrorResponse(
        @Schema(description = "Human-readable error message.", example = "Document not found: 7b9b2a0f-2b4f-4a9a-a80e-70dcda3cf9a8")
        String message
) {
}

package com.fleet.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ErrorResponse", description = "Standard API error payload.")
public record ErrorResponse(
        @Schema(description = "Human-readable error details.", example = "Authentication required")
        String message
) {
}

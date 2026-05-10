package com.fleet.document.dto;

import com.fleet.document.entity.ExtractionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Raw parser extraction stored for review.")
public record ExtractionResponse(
        UUID id,
        ExtractionStatus extractionStatus,
        String parserName,
        String parserVersion,
        Map<String, Object> rawExtractedData,
        BigDecimal extractionConfidence,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
}

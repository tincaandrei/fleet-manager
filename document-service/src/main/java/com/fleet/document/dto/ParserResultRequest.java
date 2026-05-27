package com.fleet.document.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fleet.document.entity.ParserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Parser-service result payload for a document extraction attempt.")
@JsonIgnoreProperties(ignoreUnknown = true)
public record ParserResultRequest(
        @Schema(description = "Document id. Must match the path id when present.")
        UUID documentId,

        @Schema(description = "Parser result status.", example = "PARSED", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Parser status is required")
        ParserStatus parserStatus,

        String detectedDocumentType,
        String detectedSubtype,
        BigDecimal confidence,
        String parserName,
        String parserVersion,
        Map<String, Object> extractedData,
        List<String> warnings,
        String errorCode,
        String errorMessage
) {
}

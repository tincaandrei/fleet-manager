package com.fleet.parser.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Stable parser response returned to document-service.")
public record ParserExtractionResponse(
        @Schema(example = "00000000-0000-0000-0000-000000000001")
        String documentId,

        @Schema(example = "PARSED")
        ParserStatus parserStatus,

        @Schema(example = "INSURANCE")
        DocumentType detectedDocumentType,

        @Schema(example = "RCA")
        DocumentSubtype detectedSubtype,

        @Schema(example = "0.86")
        double confidence,

        @Schema(example = "ollama-document-parser")
        String parserName,

        @Schema(example = "1.0.0")
        String parserVersion,

        @Schema(example = "PDF_TEXT")
        ExtractionMethod extractionMethod,

        @Schema(description = "Type-specific extracted data as flexible JSON.")
        JsonNode extractedData,

        List<String> warnings,

        @Schema(example = "TEXT_EXTRACTION_FAILED")
        ParserErrorCode errorCode,

        @Schema(example = "Could not extract readable text from PDF")
        String errorMessage
) {
}

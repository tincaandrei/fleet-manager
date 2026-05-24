package com.fleet.document.dto;

import com.fleet.document.entity.ParserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Schema(description = "Unapproved parser/AI extraction draft.")
public record DocumentExtractionResponse(
        String detectedDocumentType,
        String detectedSubtype,
        BigDecimal confidence,
        Map<String, Object> extractedData,
        List<String> warnings,
        String parserName,
        String parserVersion,
        ParserStatus parserStatus,
        String errorCode,
        String errorMessage
) {
}

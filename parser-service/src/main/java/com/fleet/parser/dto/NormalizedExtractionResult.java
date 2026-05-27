package com.fleet.parser.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record NormalizedExtractionResult(
        JsonNode extractedData,
        List<String> warnings,
        double llmConfidence
) {
}

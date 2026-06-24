package com.fleet.document.service.normalization;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record NormalizedExtraction(
        JsonNode extractedData,
        double llmConfidence,
        List<String> warnings
) {
}

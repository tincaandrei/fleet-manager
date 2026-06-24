package com.fleet.document.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fleet.document.dto.LlmUsageDto;

public record AiExtractionResponse(
        JsonNode extractedData,
        LlmUsageDto usage
) {
}

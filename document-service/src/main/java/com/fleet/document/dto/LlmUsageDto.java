package com.fleet.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "LLM provider usage metadata for a parser request.")
public record LlmUsageDto(
        String provider,
        String model,
        String requestId,
        Integer inputTokens,
        Integer outputTokens,
        Integer totalTokens
) {
}

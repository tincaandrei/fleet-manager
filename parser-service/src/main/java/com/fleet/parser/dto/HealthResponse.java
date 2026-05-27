package com.fleet.parser.dto;

public record HealthResponse(
        String service,
        String status,
        String ollamaBaseUrl
) {
}

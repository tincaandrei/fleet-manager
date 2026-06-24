package com.fleet.document.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public interface DocumentLlmClient {

    LlmExtractionResult extractJson(String prompt, ObjectNode expectedSchema);

    record LlmExtractionResult(JsonNode json, com.fleet.document.dto.LlmUsageDto usage) {
    }
}

package com.fleet.document.service.ai;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fleet.document.entity.DocumentType;
import com.fleet.document.entity.TextExtractionMethod;

public record AiExtractionRequest(
        String extractedText,
        TextExtractionMethod extractionMethod,
        DocumentType documentType,
        String subtype,
        ObjectNode jsonSchema,
        String promptInstructions
) {
}

package com.fleet.parser.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fleet.parser.dto.DocumentSubtype;
import com.fleet.parser.dto.DocumentType;

import java.util.List;

public interface DocumentExtractionStrategy {

    boolean supports(DocumentType type, DocumentSubtype subtype);

    DocumentType documentType();

    DocumentSubtype subtype();

    String strategyName();

    List<String> importantFields();

    JsonNode expectedSchema();

    String buildPrompt(String extractedText);

    List<String> validate(JsonNode extractedData);

    JsonNode normalize(JsonNode extractedData);
}

package com.fleet.parser.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fleet.parser.dto.ExtractionMethod;
import com.fleet.parser.strategy.DocumentExtractionStrategy;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConfidenceCalculator {

    public double calculate(DocumentExtractionStrategy strategy,
                            JsonNode extractedData,
                            ExtractionMethod extractionMethod,
                            double textQualityScore,
                            double llmConfidence,
                            List<String> validationWarnings) {
        double fieldCompletenessScore = fieldCompletenessScore(strategy.importantFields(), extractedData);
        double validationScore = validationScore(validationWarnings);
        double extractionMethodScore = extractionMethodScore(extractionMethod);
        double llmScore = llmConfidence >= 0.0 && llmConfidence <= 1.0 ? llmConfidence : 0.5;

        double confidence = (0.45 * fieldCompletenessScore)
                + (0.25 * validationScore)
                + (0.15 * extractionMethodScore)
                + (0.10 * textQualityScore)
                + (0.05 * llmScore);

        return Math.round(clamp(confidence) * 100.0) / 100.0;
    }

    private double fieldCompletenessScore(List<String> importantFields, JsonNode data) {
        if (importantFields.isEmpty()) {
            return 0.5;
        }
        long present = importantFields.stream()
                .filter(field -> hasValue(data, field))
                .count();
        return (double) present / importantFields.size();
    }

    private boolean hasValue(JsonNode data, String field) {
        if (data == null || data.isNull()) {
            return false;
        }
        JsonNode node = data.path(field);
        if (node.isMissingNode() || node.isNull()) {
            return false;
        }
        return !node.isTextual() || !node.asText().isBlank();
    }

    private double validationScore(List<String> validationWarnings) {
        if (validationWarnings == null || validationWarnings.isEmpty()) {
            return 1.0;
        }
        return Math.max(0.0, 1.0 - (validationWarnings.size() * 0.15));
    }

    private double extractionMethodScore(ExtractionMethod method) {
        if (method == null) {
            return 0.0;
        }
        return switch (method) {
            case PDF_TEXT -> 1.0;
            case OCR -> 0.8;
            case PDF_TEXT_AND_OCR -> 0.85;
        };
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}

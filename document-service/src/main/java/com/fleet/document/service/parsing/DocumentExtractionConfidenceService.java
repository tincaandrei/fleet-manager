package com.fleet.document.service.parsing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fleet.document.service.schema.DocumentExtractionSchema;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DocumentExtractionConfidenceService {

    public BigDecimal calculate(
            DocumentExtractionSchema schema,
            JsonNode extractedData,
            double textQualityScore,
            double llmConfidence,
            List<String> warnings
    ) {
        double fieldScore = schema.importantFields().isEmpty() ? 0.5 : (double) schema.importantFields().stream()
                .filter(field -> hasValue(extractedData, field))
                .count() / schema.importantFields().size();
        double validationScore = warnings == null || warnings.isEmpty() ? 1.0 : Math.max(0.0, 1.0 - (warnings.size() * 0.15));
        double llmScore = llmConfidence >= 0.0 && llmConfidence <= 1.0 ? llmConfidence : 0.5;
        double confidence = (0.45 * fieldScore) + (0.25 * validationScore) + 0.15 + (0.10 * textQualityScore) + (0.05 * llmScore);
        return BigDecimal.valueOf(Math.round(clamp(confidence) * 100.0) / 100.0);
    }

    private boolean hasValue(JsonNode data, String field) {
        if (data == null || data.isNull()) {
            return false;
        }
        JsonNode node = data.path(field);
        return !node.isMissingNode() && !node.isNull() && (!node.isTextual() || !node.asText().isBlank());
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}

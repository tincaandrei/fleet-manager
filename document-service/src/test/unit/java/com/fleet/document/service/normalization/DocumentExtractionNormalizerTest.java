package com.fleet.document.service.normalization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fleet.document.service.schema.RcaExtractionSchema;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentExtractionNormalizerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DocumentExtractionNormalizer normalizer = new DocumentExtractionNormalizer(
            objectMapper,
            new LicensePlateNormalizer(),
            new VinNormalizer(),
            new DateNormalizer(),
            new AmountNormalizer()
    );

    @Test
    void removesPlaceholderValuesAndNormalizesFields() {
        ObjectNode raw = objectMapper.createObjectNode();
        raw.put("policyNumber", "string");
        raw.put("insurerName", "number");
        raw.put("licensePlate", "b 123 abc");
        raw.put("vin", "wvw zzz 1jz 3w386752");
        raw.put("validFrom", "12.03.2026");
        raw.put("validUntil", "yyyy-MM-dd");
        raw.put("llmConfidence", 0.8);

        NormalizedExtraction result = normalizer.normalize(raw, new RcaExtractionSchema());

        assertThat(result.extractedData().path("policyNumber").isNull()).isTrue();
        assertThat(result.extractedData().path("insurerName").isNull()).isTrue();
        assertThat(result.extractedData().path("licensePlate").asText()).isEqualTo("B123ABC");
        assertThat(result.extractedData().path("vin").asText()).isEqualTo("WVWZZZ1JZ3W386752");
        assertThat(result.extractedData().path("validFrom").asText()).isEqualTo("2026-03-12");
        assertThat(result.extractedData().path("validUntil").isNull()).isTrue();
        assertThat(result.llmConfidence()).isEqualTo(0.8);
    }

    @Test
    void malformedDateBecomesNullWithWarning() {
        ObjectNode raw = objectMapper.createObjectNode();
        raw.put("licensePlate", "B123ABC");
        raw.put("validUntil", "not-a-date");

        NormalizedExtraction result = normalizer.normalize(raw, new RcaExtractionSchema());

        assertThat(result.extractedData().path("validUntil").isNull()).isTrue();
        assertThat(result.warnings()).contains("Invalid date format for validUntil");
    }
}

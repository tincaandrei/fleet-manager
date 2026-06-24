package com.fleet.document.service.normalization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fleet.document.service.schema.DocumentExtractionSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DocumentExtractionNormalizer {

    private static final Set<String> PLACEHOLDERS = Set.of(
            "string", "number", "yyyy-mm-dd", "null", "-", "n/a", "na"
    );
    private static final Set<String> CURRENCIES = Set.of("RON", "EUR", "USD");
    private static final Set<String> DATE_FIELDS = Set.of(
            "validFrom", "validUntil", "inspectionDate", "invoiceDate", "issueDate"
    );
    private static final Set<String> NUMBER_FIELDS = Set.of(
            "amount", "totalAmount", "vatAmount", "odometerKm", "quantity", "unitPrice", "totalPrice"
    );
    private static final Map<String, String> EXPENSE_CATEGORY_ALIASES = Map.ofEntries(
            Map.entry("COMBUSTIBIL", "FUEL"),
            Map.entry("FUEL", "FUEL"),
            Map.entry("SERVICE", "SERVICE"),
            Map.entry("REPARATIE", "SERVICE"),
            Map.entry("REPARATII", "SERVICE"),
            Map.entry("ANVELOPE", "TIRE_REPLACEMENT"),
            Map.entry("TIRE_REPLACEMENT", "TIRE_REPLACEMENT"),
            Map.entry("PARTS", "PARTS"),
            Map.entry("PIESE", "PARTS"),
            Map.entry("SPALATORIE", "CAR_WASH"),
            Map.entry("CAR_WASH", "CAR_WASH"),
            Map.entry("PARCARE", "PARKING"),
            Map.entry("PARKING", "PARKING"),
            Map.entry("ROVINIETA", "ROAD_TAX"),
            Map.entry("ROVIGNETA", "ROAD_TAX"),
            Map.entry("ROAD_TAX", "ROAD_TAX"),
            Map.entry("ASIGURARE", "INSURANCE"),
            Map.entry("RCA", "INSURANCE"),
            Map.entry("INSURANCE", "INSURANCE"),
            Map.entry("OTHER_EXPENS", "OTHER_EXPENSE"),
            Map.entry("OTHER_EXPENSE", "OTHER_EXPENSE")
    );

    private final ObjectMapper objectMapper;
    private final LicensePlateNormalizer licensePlateNormalizer;
    private final VinNormalizer vinNormalizer;
    private final DateNormalizer dateNormalizer;
    private final AmountNormalizer amountNormalizer;

    public NormalizedExtraction normalize(JsonNode rawData, DocumentExtractionSchema schema) {
        JsonNode source = unwrap(rawData);
        List<String> warnings = new ArrayList<>();
        double llmConfidence = extractLlmConfidence(source);
        ObjectNode normalized = objectMapper.createObjectNode();

        for (String field : schema.relevantFields()) {
            if ("llmConfidence".equals(field)) {
                continue;
            }
            JsonNode value = source == null || !source.isObject() ? null : source.get(field);
            normalized.set(field, normalizeField(field, value, warnings));
        }

        return new NormalizedExtraction(normalized, llmConfidence, warnings);
    }

    private JsonNode normalizeField(String field, JsonNode value, List<String> warnings) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return objectMapper.nullNode();
        }
        if (value.isTextual() && isPlaceholder(value.asText())) {
            return objectMapper.nullNode();
        }
        if ("licensePlate".equals(field)) {
            return normalizeLicensePlate(value, warnings);
        }
        if ("vin".equals(field)) {
            return normalizeVin(value, warnings);
        }
        if (DATE_FIELDS.contains(field)) {
            return normalizeDate(field, value, warnings);
        }
        if ("currency".equals(field)) {
            return normalizeCurrency(value, warnings);
        }
        if ("expenseCategory".equals(field)) {
            return normalizeExpenseCategory(value);
        }
        if ("items".equals(field)) {
            return normalizeItems(value, warnings);
        }
        if (NUMBER_FIELDS.contains(field)) {
            return normalizeNumber(field, value, warnings);
        }
        return value;
    }

    private JsonNode normalizeLicensePlate(JsonNode value, List<String> warnings) {
        String normalized = licensePlateNormalizer.normalize(value.asText());
        if (normalized == null) {
            return objectMapper.nullNode();
        }
        if (!licensePlateNormalizer.isValidRomanianPlate(normalized)) {
            warnings.add("License plate appears malformed");
            return objectMapper.nullNode();
        }
        return objectMapper.getNodeFactory().textNode(normalized);
    }

    private JsonNode normalizeVin(JsonNode value, List<String> warnings) {
        String normalized = vinNormalizer.normalize(value.asText());
        if (normalized == null) {
            return objectMapper.nullNode();
        }
        if (!vinNormalizer.isValidVin(normalized)) {
            warnings.add("VIN appears malformed");
        }
        return objectMapper.getNodeFactory().textNode(normalized);
    }

    private JsonNode normalizeDate(String field, JsonNode value, List<String> warnings) {
        String normalized = dateNormalizer.normalize(value.asText());
        if (normalized == null) {
            warnings.add("Invalid date format for " + field);
            return objectMapper.nullNode();
        }
        return objectMapper.getNodeFactory().textNode(normalized);
    }

    private JsonNode normalizeCurrency(JsonNode value, List<String> warnings) {
        String normalized = value.asText().trim().toUpperCase(Locale.ROOT);
        if (!CURRENCIES.contains(normalized)) {
            warnings.add("Invalid currency");
            return objectMapper.nullNode();
        }
        return objectMapper.getNodeFactory().textNode(normalized);
    }

    private JsonNode normalizeExpenseCategory(JsonNode value) {
        String normalized = value.asText()
                .trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
        return objectMapper.getNodeFactory().textNode(EXPENSE_CATEGORY_ALIASES.getOrDefault(normalized, normalized));
    }

    private JsonNode normalizeNumber(String field, JsonNode value, List<String> warnings) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return objectMapper.nullNode();
        }
        if (value.isNumber()) {
            return value;
        }
        BigDecimal normalized = amountNormalizer.normalize(value.asText());
        if (normalized == null) {
            warnings.add("Invalid number format for " + field);
            return objectMapper.nullNode();
        }
        return objectMapper.getNodeFactory().numberNode(normalized);
    }

    private JsonNode normalizeItems(JsonNode value, List<String> warnings) {
        if (!value.isArray()) {
            return objectMapper.nullNode();
        }
        ArrayNode items = objectMapper.createArrayNode();
        for (JsonNode rawItem : value) {
            ObjectNode item = objectMapper.createObjectNode();
            item.set("description", normalizeSimpleText(rawItem.path("description")));
            item.set("quantity", normalizeNumber("quantity", rawItem.path("quantity"), warnings));
            item.set("unit", normalizeSimpleText(rawItem.path("unit")));
            item.set("unitPrice", normalizeNumber("unitPrice", rawItem.path("unitPrice"), warnings));
            item.set("totalPrice", normalizeNumber("totalPrice", rawItem.path("totalPrice"), warnings));
            items.add(item);
        }
        return items;
    }

    private JsonNode normalizeSimpleText(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull() || (value.isTextual() && isPlaceholder(value.asText()))) {
            return objectMapper.nullNode();
        }
        return value;
    }

    private boolean isPlaceholder(String value) {
        if (value == null) {
            return true;
        }
        return PLACEHOLDERS.contains(value.trim().toLowerCase(Locale.ROOT));
    }

    private double extractLlmConfidence(JsonNode data) {
        if (data != null && data.has("llmConfidence") && data.get("llmConfidence").isNumber()) {
            return data.get("llmConfidence").asDouble();
        }
        return 0.5;
    }

    private JsonNode unwrap(JsonNode rawData) {
        if (rawData != null && rawData.has("extractedData") && rawData.get("extractedData").isObject()) {
            return rawData.get("extractedData");
        }
        return rawData;
    }
}

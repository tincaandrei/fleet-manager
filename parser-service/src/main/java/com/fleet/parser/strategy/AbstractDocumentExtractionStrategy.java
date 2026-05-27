package com.fleet.parser.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fleet.parser.dto.DocumentSubtype;
import com.fleet.parser.dto.DocumentType;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

abstract class AbstractDocumentExtractionStrategy implements DocumentExtractionStrategy {

    private static final Pattern VIN_PATTERN = Pattern.compile("^[A-HJ-NPR-Z0-9]{17}$");

    protected final ObjectMapper objectMapper;

    protected AbstractDocumentExtractionStrategy(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(DocumentType type, DocumentSubtype subtype) {
        return documentType() == type && (subtype() == subtype || subtype() == DocumentSubtype.UNKNOWN);
    }

    @Override
    public String buildPrompt(String extractedText) {
        return """
                You extract structured data from Romanian vehicle-related documents.
                Return only valid JSON. Do not wrap the answer in Markdown.
                Do not invent values. Use null for missing or uncertain fields.
                Normalize dates to yyyy-MM-dd where possible.
                Normalize license plates without spaces where possible.
                Keep uncertain values null.
                Include llmConfidence as a number between 0 and 1 if you can estimate it.

                Document type: %s
                Document subtype: %s
                Expected JSON schema:
                %s

                Document text:
                %s
                """.formatted(documentType(), subtype(), expectedSchema().toPrettyString(), limitText(extractedText));
    }

    @Override
    public JsonNode normalize(JsonNode extractedData) {
        ObjectNode copy = objectMapper.createObjectNode();
        if (extractedData != null && extractedData.isObject()) {
            extractedData.fields().forEachRemaining(entry -> copy.set(entry.getKey(), entry.getValue()));
        }

        normalizeUpperNoSpaces(copy, "licensePlate");
        normalizeUpperNoSpaces(copy, "vin");
        normalizeExpenseCategory(copy);

        for (String field : dateFields()) {
            normalizeDate(copy, field);
        }
        return copy;
    }

    @Override
    public List<String> validate(JsonNode extractedData) {
        List<String> warnings = new ArrayList<>();
        for (String field : importantFields()) {
            if (!hasValue(extractedData, field)) {
                warnings.add("Missing important field: " + field);
            }
        }

        for (String field : dateFields()) {
            if (hasValue(extractedData, field) && !isIsoDate(extractedData.path(field).asText())) {
                warnings.add("Invalid date format for " + field);
            }
        }

        if (hasValue(extractedData, "vin") && !VIN_PATTERN.matcher(extractedData.path("vin").asText()).matches()) {
            warnings.add("VIN appears malformed");
        }
        if (requiresVehicleIdentifier() && !hasValue(extractedData, "licensePlate") && !hasValue(extractedData, "vin")) {
            warnings.add("Missing both licensePlate and vin");
        }
        return warnings;
    }

    protected ObjectNode schema(Map<String, String> fields) {
        ObjectNode schema = objectMapper.createObjectNode();
        fields.forEach(schema::put);
        schema.put("llmConfidence", "number|null");
        return schema;
    }

    protected boolean hasValue(JsonNode data, String field) {
        if (data == null || data.isNull()) {
            return false;
        }
        JsonNode node = data.path(field);
        return !node.isMissingNode() && !node.isNull() && (!node.isTextual() || !node.asText().isBlank());
    }

    protected List<String> dateFields() {
        return List.of("validFrom", "validUntil", "inspectionDate", "invoiceDate");
    }

    protected boolean requiresVehicleIdentifier() {
        return true;
    }

    private void normalizeUpperNoSpaces(ObjectNode node, String field) {
        if (node.hasNonNull(field) && node.get(field).isTextual()) {
            String normalized = node.get(field).asText()
                    .replaceAll("[^A-Za-z0-9]", "")
                    .toUpperCase(Locale.ROOT);
            if (!normalized.isBlank()) {
                node.put(field, normalized);
            }
        }
    }

    private void normalizeExpenseCategory(ObjectNode node) {
        if (!node.hasNonNull("expenseCategory") || !node.get("expenseCategory").isTextual()) {
            return;
        }
        String value = node.get("expenseCategory").asText()
                .trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
        if ("OTHER_EXPENS".equals(value)) {
            value = "OTHER_EXPENSE";
        }
        node.put("expenseCategory", value);
    }

    private void normalizeDate(ObjectNode node, String field) {
        if (!node.hasNonNull(field) || !node.get(field).isTextual()) {
            return;
        }
        String value = node.get(field).asText().trim();
        if (isIsoDate(value)) {
            node.put(field, value);
        }
    }

    private boolean isIsoDate(String value) {
        try {
            LocalDate.parse(value);
            return true;
        } catch (DateTimeParseException exception) {
            return false;
        }
    }

    private String limitText(String text) {
        if (text == null) {
            return "";
        }
        int maxCharacters = 12_000;
        if (text.length() <= maxCharacters) {
            return text;
        }
        return text.substring(0, maxCharacters) + "\n[TRUNCATED]";
    }
}

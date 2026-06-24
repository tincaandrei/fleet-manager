package com.fleet.document.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fleet.document.dto.ParserResultRequest;
import com.fleet.document.entity.DocumentType;
import com.fleet.document.entity.ParserStatus;
import com.fleet.document.entity.VehicleDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class DocumentParsingService {

    private static final Pattern VIN_PATTERN = Pattern.compile("^[A-HJ-NPR-Z0-9]{17}$");
    private static final Pattern ROMANIAN_LICENSE_PLATE_PATTERN = Pattern.compile("^(?:B\\d{2,3}[A-Z]{3}|[A-Z]{2}\\d{2,3}[A-Z]{3})$");
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final DocumentTextExtractionService textExtractionService;
    private final DocumentLlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final String parserName;
    private final String parserVersion;

    public DocumentParsingService(
            DocumentTextExtractionService textExtractionService,
            DocumentLlmClient llmClient,
            ObjectMapper objectMapper,
            @Value("${document.parser.name}") String parserName,
            @Value("${document.parser.version}") String parserVersion
    ) {
        this.textExtractionService = textExtractionService;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
        this.parserName = parserName;
        this.parserVersion = parserVersion;
    }

    public ParserResultRequest parse(VehicleDocument document) {
        try {
            ExtractedTextResult textExtraction = textExtractionService.extractText(document);

            DocumentTypeDetection detection = detect(textExtraction.text());
            ExtractionStrategy strategy = strategyFor(detection);
            DocumentLlmClient.LlmExtractionResult llmResult = llmClient.extractJson(
                    strategy.buildPrompt(textExtraction.text()),
                    strategy.expectedSchema()
            );
            JsonNode rawModelData = unwrapExtractedData(llmResult.json());
            NormalizedExtraction normalized = normalizeAndValidate(strategy, rawModelData);
            List<String> warnings = new ArrayList<>();
            if (textExtraction.warnings() != null) {
                warnings.addAll(textExtraction.warnings());
            }
            warnings.addAll(normalized.warnings());

            BigDecimal confidence = BigDecimal.valueOf(calculateConfidence(
                    strategy.importantFields(),
                    normalized.extractedData(),
                    textExtraction.textQualityScore(),
                    normalized.llmConfidence(),
                    warnings
            ));

            return new ParserResultRequest(
                    document.getId(),
                    ParserStatus.PARSED,
                    detection.documentType().name(),
                    detection.subtype(),
                    confidence,
                    parserName,
                    parserVersion,
                    textExtraction.extractionMethod(),
                    llmResult.usage(),
                    objectMapper.convertValue(normalized.extractedData(), MAP_TYPE),
                    warnings,
                    null,
                    null
            );
        } catch (TextExtractionException exception) {
            return failure(document.getId(), exception.errorCode(), exception.getMessage());
        } catch (DocumentLlmException exception) {
            return failure(document.getId(), exception.errorCode(), exception.getMessage());
        } catch (Exception exception) {
            return failure(document.getId(), "INTERNAL_ERROR", "Unexpected document parser error");
        }
    }

    DocumentTypeDetection detect(String text) {
        String normalized = normalizeForDetection(text);
        if (containsAny(normalized, "rovigneta", "rovinieta", "roviniete", "erovinieta", "tarif de utilizare", "cnadnr")) {
            return new DocumentTypeDetection(DocumentType.ROAD_TAX, "ROVINIETA");
        }
        if (containsAny(normalized, "asigurare", "asigurari", "asigurator", "polita", "carte verde", "bonus malus", "raspundere civila auto")
                || containsAnyToken(normalized, "rca")) {
            return new DocumentTypeDetection(DocumentType.INSURANCE, "RCA");
        }
        if (containsAny(normalized, "factura", "bon fiscal", "invoice", "receipt", "tva")) {
            return new DocumentTypeDetection(DocumentType.EXPENSE_INVOICE, "UNKNOWN");
        }
        if (containsAny(normalized, "inspectie tehnica periodica", "valabilitate inspectie")
                || containsAnyToken(normalized, "itp", "rar")) {
            return new DocumentTypeDetection(DocumentType.TECHNICAL_INSPECTION, "ITP");
        }
        return new DocumentTypeDetection(DocumentType.OTHER, "UNKNOWN");
    }

    private ExtractionStrategy strategyFor(DocumentTypeDetection detection) {
        return switch (detection.documentType()) {
            case INSURANCE -> strategy(detection, List.of("policyNumber", "insurerName", "licensePlate", "vin", "validFrom", "validUntil"), Map.of(
                    "policyNumber", "string|null",
                    "insurerName", "string|null",
                    "ownerName", "string|null",
                    "licensePlate", "string|null",
                    "vin", "string|null",
                    "validFrom", "yyyy-MM-dd|null",
                    "validUntil", "yyyy-MM-dd|null"
            ), true);
            case TECHNICAL_INSPECTION -> strategy(detection, List.of("inspectionNumber", "stationName", "licensePlate", "vin", "inspectionDate", "validUntil"), Map.of(
                    "inspectionNumber", "string|null",
                    "stationName", "string|null",
                    "licensePlate", "string|null",
                    "vin", "string|null",
                    "inspectionDate", "yyyy-MM-dd|null",
                    "validUntil", "yyyy-MM-dd|null"
            ), true);
            case ROAD_TAX -> strategy(detection, List.of("licensePlate", "vin", "category", "validFrom", "validUntil", "issuer"), Map.of(
                    "licensePlate", "string|null",
                    "vin", "string|null",
                    "category", "string|null",
                    "validFrom", "yyyy-MM-dd|null",
                    "validUntil", "yyyy-MM-dd|null",
                    "issuer", "string|null",
                    "transactionId", "string|null",
                    "amount", "number|null",
                    "currency", "RON|EUR|USD|null"
            ), true, """
                    Rovinieta-specific rules:
                    - This is a Romanian road tax document, even if the text also contains factura, invoice, TVA, or total.
                    - Extract the vehicle registration plate only from labels such as numar inmatriculare, nr. inmatriculare, vehicul, or registration number.
                    - A Romanian license plate looks like B123ABC, B12ABC, CJ12ABC, CJ123ABC. Do not use transaction ids, invoice ids, serial numbers, CNADNR ids, CUI, CIF, or order numbers as licensePlate.
                    - Values starting with CNADNR are transaction or issuer references, not vehicle registration plates.
                    - If no clear registration plate is present, return licensePlate as null.
                    - Put transaction/order/reference identifiers in transactionId when present.
                    """);
            case EXPENSE_INVOICE -> strategy(detection, List.of("invoiceNumber", "supplierName", "invoiceDate", "totalAmount", "currency", "expenseCategory"), Map.ofEntries(
                    Map.entry("invoiceNumber", "string|null"),
                    Map.entry("supplierName", "string|null"),
                    Map.entry("supplierTaxId", "string|null"),
                    Map.entry("invoiceDate", "yyyy-MM-dd|null"),
                    Map.entry("totalAmount", "number|null"),
                    Map.entry("currency", "RON|EUR|USD|null"),
                    Map.entry("vatAmount", "number|null"),
                    Map.entry("expenseCategory", "FUEL|SERVICE|TIRE_REPLACEMENT|PARTS|CAR_WASH|PARKING|OTHER_EXPENSE|null"),
                    Map.entry("licensePlate", "string|null"),
                    Map.entry("vin", "string|null"),
                    Map.entry("odometerKm", "number|null"),
                    Map.entry("items", "array of {description, quantity, unit, unitPrice, totalPrice}")
            ), false);
            case OTHER -> strategy(detection, List.of(), Map.of(
                    "documentTitle", "string|null",
                    "licensePlate", "string|null",
                    "vin", "string|null",
                    "issueDate", "yyyy-MM-dd|null",
                    "summary", "string|null"
            ), false);
        };
    }

    private ExtractionStrategy strategy(
            DocumentTypeDetection detection,
            List<String> importantFields,
            Map<String, String> schemaFields,
            boolean requiresVehicleIdentifier
    ) {
        return strategy(detection, importantFields, schemaFields, requiresVehicleIdentifier, "");
    }

    private ExtractionStrategy strategy(
            DocumentTypeDetection detection,
            List<String> importantFields,
            Map<String, String> schemaFields,
            boolean requiresVehicleIdentifier,
            String extractionRules
    ) {
        ObjectNode schema = objectMapper.createObjectNode();
        schemaFields.forEach(schema::put);
        schema.put("llmConfidence", "number|null");
        return new ExtractionStrategy(detection.documentType(), detection.subtype(), importantFields, schema, requiresVehicleIdentifier, extractionRules);
    }

    private NormalizedExtraction normalizeAndValidate(ExtractionStrategy strategy, JsonNode rawData) {
        double llmConfidence = extractLlmConfidence(rawData);
        ObjectNode normalized = objectMapper.createObjectNode();
        if (rawData != null && rawData.isObject()) {
            rawData.fields().forEachRemaining(entry -> normalized.set(entry.getKey(), entry.getValue()));
        }
        normalized.remove("llmConfidence");
        normalizeUpperNoSpaces(normalized, "licensePlate");
        removeMalformedLicensePlate(normalized);
        normalizeUpperNoSpaces(normalized, "vin");
        normalizeExpenseCategory(normalized);
        for (String field : List.of("validFrom", "validUntil", "inspectionDate", "invoiceDate", "issueDate")) {
            normalizeDate(normalized, field);
        }
        return new NormalizedExtraction(normalized, validate(strategy, normalized), llmConfidence);
    }

    private List<String> validate(ExtractionStrategy strategy, JsonNode extractedData) {
        List<String> warnings = new ArrayList<>();
        for (String field : strategy.importantFields()) {
            if (!hasValue(extractedData, field)) {
                warnings.add("Missing important field: " + field);
            }
        }
        for (String field : List.of("validFrom", "validUntil", "inspectionDate", "invoiceDate", "issueDate")) {
            if (hasValue(extractedData, field) && !isIsoDate(extractedData.path(field).asText())) {
                warnings.add("Invalid date format for " + field);
            }
        }
        if (hasValue(extractedData, "vin") && !VIN_PATTERN.matcher(extractedData.path("vin").asText()).matches()) {
            warnings.add("VIN appears malformed");
        }
        if (hasValue(extractedData, "licensePlate")
                && !ROMANIAN_LICENSE_PLATE_PATTERN.matcher(extractedData.path("licensePlate").asText()).matches()) {
            warnings.add("License plate appears malformed");
        }
        if (strategy.requiresVehicleIdentifier() && !hasValue(extractedData, "licensePlate") && !hasValue(extractedData, "vin")) {
            warnings.add("Missing both licensePlate and vin");
        }
        return warnings;
    }

    private ParserResultRequest failure(UUID documentId, String errorCode, String errorMessage) {
        return new ParserResultRequest(
                documentId,
                ParserStatus.FAILED,
                null,
                null,
                BigDecimal.ZERO,
                parserName,
                parserVersion,
                null,
                null,
                null,
                List.of(errorMessage),
                errorCode,
                errorMessage
        );
    }

    private JsonNode unwrapExtractedData(JsonNode modelData) {
        if (modelData != null && modelData.has("extractedData") && modelData.get("extractedData").isObject()) {
            return modelData.get("extractedData");
        }
        return modelData;
    }

    private double calculateConfidence(
            List<String> importantFields,
            JsonNode extractedData,
            double textQualityScore,
            double llmConfidence,
            List<String> warnings
    ) {
        double fieldScore = importantFields.isEmpty() ? 0.5 : (double) importantFields.stream()
                .filter(field -> hasValue(extractedData, field))
                .count() / importantFields.size();
        double validationScore = warnings == null || warnings.isEmpty() ? 1.0 : Math.max(0.0, 1.0 - (warnings.size() * 0.15));
        double llmScore = llmConfidence >= 0.0 && llmConfidence <= 1.0 ? llmConfidence : 0.5;
        double confidence = (0.45 * fieldScore) + (0.25 * validationScore) + 0.15 + (0.10 * textQualityScore) + (0.05 * llmScore);
        return Math.round(clamp(confidence) * 100.0) / 100.0;
    }

    private String normalizeForDetection(String value) {
        if (value == null) {
            return "";
        }
        String noDiacritics = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noDiacritics.toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAnyToken(String value, String... tokens) {
        for (String token : tokens) {
            if (Pattern.compile("(?<![a-z0-9])" + Pattern.quote(token) + "(?![a-z0-9])").matcher(value).find()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasValue(JsonNode data, String field) {
        if (data == null || data.isNull()) {
            return false;
        }
        JsonNode node = data.path(field);
        return !node.isMissingNode() && !node.isNull() && (!node.isTextual() || !node.asText().isBlank());
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

    private void removeMalformedLicensePlate(ObjectNode node) {
        if (!node.hasNonNull("licensePlate") || !node.get("licensePlate").isTextual()) {
            return;
        }
        String value = node.get("licensePlate").asText();
        if (!ROMANIAN_LICENSE_PLATE_PATTERN.matcher(value).matches()) {
            node.putNull("licensePlate");
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
            return;
        }
        if (value.length() >= 10 && isIsoDate(value.substring(0, 10))) {
            node.put(field, value.substring(0, 10));
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

    private double extractLlmConfidence(JsonNode data) {
        if (data != null && data.has("llmConfidence") && data.get("llmConfidence").isNumber()) {
            return data.get("llmConfidence").asDouble();
        }
        return 0.5;
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    record DocumentTypeDetection(DocumentType documentType, String subtype) {
    }

    private record NormalizedExtraction(JsonNode extractedData, List<String> warnings, double llmConfidence) {
    }

    private record ExtractionStrategy(
            DocumentType documentType,
            String subtype,
            List<String> importantFields,
            ObjectNode expectedSchema,
            boolean requiresVehicleIdentifier,
            String extractionRules
    ) {
        private String buildPrompt(String extractedText) {
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

                    Extraction rules:
                    %s

                    Document text:
                    %s
                    """.formatted(documentType, subtype, expectedSchema.toPrettyString(), extractionRules, limitText(extractedText));
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

}

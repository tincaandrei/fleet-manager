package com.fleet.document.service.parsing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleet.document.dto.ParserResultRequest;
import com.fleet.document.entity.ParserStatus;
import com.fleet.document.entity.VehicleDocument;
import com.fleet.document.service.ai.AiExtractionRequest;
import com.fleet.document.service.ai.AiExtractionResponse;
import com.fleet.document.service.ai.DocumentAiClient;
import com.fleet.document.service.ai.DocumentAiException;
import com.fleet.document.service.normalization.DocumentExtractionNormalizer;
import com.fleet.document.service.normalization.NormalizedExtraction;
import com.fleet.document.service.schema.DocumentExtractionSchema;
import com.fleet.document.service.schema.DocumentExtractionSchemaRegistry;
import com.fleet.document.service.validation.DocumentExtractionValidator;
import com.fleet.document.service.validation.ValidationResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DocumentParsingOrchestrator {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final DocumentTextExtractionService textExtractionService;
    private final DocumentTypeDetector documentTypeDetector;
    private final DocumentExtractionSchemaRegistry schemaRegistry;
    private final DocumentAiClient aiClient;
    private final DocumentExtractionNormalizer normalizer;
    private final DocumentExtractionValidator validator;
    private final DocumentExtractionConfidenceService confidenceService;
    private final ObjectMapper objectMapper;
    private final String parserName;
    private final String parserVersion;

    public DocumentParsingOrchestrator(
            DocumentTextExtractionService textExtractionService,
            DocumentTypeDetector documentTypeDetector,
            DocumentExtractionSchemaRegistry schemaRegistry,
            DocumentAiClient aiClient,
            DocumentExtractionNormalizer normalizer,
            DocumentExtractionValidator validator,
            DocumentExtractionConfidenceService confidenceService,
            ObjectMapper objectMapper,
            @Value("${document.parser.name}") String parserName,
            @Value("${document.parser.version}") String parserVersion
    ) {
        this.textExtractionService = textExtractionService;
        this.documentTypeDetector = documentTypeDetector;
        this.schemaRegistry = schemaRegistry;
        this.aiClient = aiClient;
        this.normalizer = normalizer;
        this.validator = validator;
        this.confidenceService = confidenceService;
        this.objectMapper = objectMapper;
        this.parserName = parserName;
        this.parserVersion = parserVersion;
    }

    public ParserResultRequest parse(VehicleDocument document) {
        try {
            ExtractedTextResult text = textExtractionService.extractText(document);
            DocumentTypeDetection detection = documentTypeDetector.detect(text.text());
            DocumentExtractionSchema schema = schemaRegistry.schemaFor(detection);

            AiExtractionResponse ai = aiClient.extract(new AiExtractionRequest(
                    text.text(),
                    text.extractionMethod(),
                    detection.documentType(),
                    detection.subtype(),
                    schema.jsonSchema(),
                    schema.promptInstructions()
            ));

            NormalizedExtraction normalized = normalizer.normalize(ai.extractedData(), schema);
            ValidationResult validation = validator.validate(normalized.extractedData(), schema, text);
            List<String> warnings = combineWarnings(text.warnings(), normalized.warnings(), validation.warnings());

            BigDecimal confidence = confidenceService.calculate(
                    schema,
                    normalized.extractedData(),
                    text.textQualityScore(),
                    normalized.llmConfidence(),
                    warnings
            );

            return new ParserResultRequest(
                    document.getId(),
                    ParserStatus.PARSED,
                    detection.documentType().name(),
                    detection.subtype(),
                    confidence,
                    parserName,
                    parserVersion,
                    text.extractionMethod(),
                    ai.usage(),
                    objectMapper.convertValue(normalized.extractedData(), MAP_TYPE),
                    warnings,
                    null,
                    null
            );
        } catch (TextExtractionException exception) {
            return failure(document.getId(), exception.errorCode(), exception.getMessage());
        } catch (DocumentAiException exception) {
            return failure(document.getId(), exception.errorCode(), exception.getMessage());
        } catch (Exception exception) {
            return failure(document.getId(), "INTERNAL_ERROR", "Unexpected document parser error");
        }
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

    @SafeVarargs
    private List<String> combineWarnings(List<String>... warningGroups) {
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        for (List<String> group : warningGroups) {
            if (group == null) {
                continue;
            }
            for (String warning : group) {
                if (warning != null && !warning.isBlank()) {
                    warnings.add(warning.trim());
                }
            }
        }
        return new ArrayList<>(warnings);
    }
}

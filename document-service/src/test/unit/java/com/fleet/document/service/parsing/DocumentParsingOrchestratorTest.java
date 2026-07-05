package com.fleet.document.service.parsing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fleet.document.dto.LlmUsageDto;
import com.fleet.document.dto.ParserResultRequest;
import com.fleet.document.entity.DocumentType;
import com.fleet.document.entity.ParserStatus;
import com.fleet.document.entity.TextExtractionMethod;
import com.fleet.document.entity.VehicleDocument;
import com.fleet.document.service.ai.AiExtractionResponse;
import com.fleet.document.service.ai.DocumentAiClient;
import com.fleet.document.service.ai.DocumentAiException;
import com.fleet.document.service.normalization.AmountNormalizer;
import com.fleet.document.service.normalization.DateNormalizer;
import com.fleet.document.service.normalization.DocumentExtractionNormalizer;
import com.fleet.document.service.normalization.LicensePlateNormalizer;
import com.fleet.document.service.normalization.VinNormalizer;
import com.fleet.document.service.schema.DocumentExtractionSchemaRegistry;
import com.fleet.document.service.schema.RcaExtractionSchema;
import com.fleet.document.service.validation.DocumentExtractionValidator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentParsingOrchestratorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DocumentTextExtractionService textExtractionService = mock(DocumentTextExtractionService.class);
    private final DocumentTypeDetector documentTypeDetector = mock(DocumentTypeDetector.class);
    private final DocumentExtractionSchemaRegistry schemaRegistry = mock(DocumentExtractionSchemaRegistry.class);
    private final DocumentAiClient aiClient = mock(DocumentAiClient.class);
    private final RcaExtractionSchema schema = new RcaExtractionSchema();
    private final DocumentParsingOrchestrator orchestrator = new DocumentParsingOrchestrator(
            textExtractionService,
            documentTypeDetector,
            schemaRegistry,
            aiClient,
            new DocumentExtractionNormalizer(
                    objectMapper,
                    new LicensePlateNormalizer(),
                    new VinNormalizer(),
                    new DateNormalizer(),
                    new AmountNormalizer()
            ),
            new DocumentExtractionValidator(new LicensePlateNormalizer(), new VinNormalizer(), new DateNormalizer()),
            new DocumentExtractionConfidenceService(),
            objectMapper,
            "test-parser",
            "test"
    );

    @Test
    void happyPathReturnsParsedResultWithRelevantSchemaFieldsOnly() {
        VehicleDocument document = document();
        LlmUsageDto usage = new LlmUsageDto("openai", "gpt-5.4-mini", "resp_123", 120, 60, 180);
        ObjectNode aiJson = objectMapper.createObjectNode();
        aiJson.put("policyNumber", "RCA-123");
        aiJson.put("insurerName", "Test SA");
        aiJson.put("licensePlate", "b 123 abc");
        aiJson.put("validUntil", "2027-03-10");
        aiJson.put("llmConfidence", 0.8);
        aiJson.put("unexpectedField", "must disappear");

        when(textExtractionService.extractText(document)).thenReturn(new ExtractedTextResult(
                "Polita RCA asigurator Test SA B123ABC",
                TextExtractionMethod.PDFBOX,
                1,
                List.of(),
                0.9
        ));
        when(documentTypeDetector.detect(any(String.class))).thenReturn(new DocumentTypeDetection(
                DocumentType.INSURANCE,
                "RCA",
                0.9,
                List.of("rca")
        ));
        when(schemaRegistry.schemaFor(any(DocumentTypeDetection.class))).thenReturn(schema);
        when(aiClient.extract(any())).thenReturn(new AiExtractionResponse(aiJson, usage));

        ParserResultRequest result = orchestrator.parse(document);

        assertThat(result.parserStatus()).isEqualTo(ParserStatus.PARSED);
        assertThat(result.detectedDocumentType()).isEqualTo("INSURANCE");
        assertThat(result.detectedSubtype()).isEqualTo("RCA");
        assertThat(result.llmUsage()).isEqualTo(usage);
        assertThat(result.extractedData()).containsEntry("licensePlate", "B123ABC");
        assertThat(result.extractedData()).doesNotContainKey("llmConfidence");
        assertThat(result.extractedData()).doesNotContainKey("unexpectedField");
    }

    @Test
    void textExtractionFailureReturnsFailedResult() {
        VehicleDocument document = document();
        when(textExtractionService.extractText(document)).thenThrow(new TextExtractionException(
                "OCR_FAILED",
                "OCR failed while reading the PDF"
        ));

        ParserResultRequest result = orchestrator.parse(document);

        assertThat(result.parserStatus()).isEqualTo(ParserStatus.FAILED);
        assertThat(result.errorCode()).isEqualTo("OCR_FAILED");
    }

    @Test
    void openAiFailureReturnsFailedResult() {
        VehicleDocument document = document();
        when(textExtractionService.extractText(document)).thenReturn(new ExtractedTextResult(
                "Polita RCA asigurator",
                TextExtractionMethod.PDFBOX,
                1,
                List.of(),
                0.9
        ));
        when(documentTypeDetector.detect(any(String.class))).thenReturn(new DocumentTypeDetection(
                DocumentType.INSURANCE,
                "RCA",
                0.9,
                List.of("rca")
        ));
        when(schemaRegistry.schemaFor(any(DocumentTypeDetection.class))).thenReturn(schema);
        when(aiClient.extract(any())).thenThrow(new DocumentAiException("OPENAI_UNAVAILABLE", "OpenAI request failed"));

        ParserResultRequest result = orchestrator.parse(document);

        assertThat(result.parserStatus()).isEqualTo(ParserStatus.FAILED);
        assertThat(result.errorCode()).isEqualTo("OPENAI_UNAVAILABLE");
    }

    private VehicleDocument document() {
        VehicleDocument document = new VehicleDocument();
        document.setId(UUID.randomUUID());
        return document;
    }
}

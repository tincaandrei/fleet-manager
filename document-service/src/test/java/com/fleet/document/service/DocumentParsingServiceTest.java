package com.fleet.document.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fleet.document.dto.LlmUsageDto;
import com.fleet.document.dto.ParserResultRequest;
import com.fleet.document.entity.DocumentType;
import com.fleet.document.entity.ParserStatus;
import com.fleet.document.entity.TextExtractionMethod;
import com.fleet.document.entity.VehicleDocument;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentParsingServiceTest {

    private final DocumentParsingService parsingService = new DocumentParsingService(
            mock(DocumentTextExtractionService.class),
            mock(DocumentLlmClient.class),
            new ObjectMapper(),
            "test-parser",
            "test"
    );

    @Test
    void detectsRcaTemplateAsInsurance() {
        DocumentParsingService.DocumentTypeDetection detection = parsingService.detect("""
                GROUPAMA ASIGURARI S.A.
                CONTRACT DE ASIGURARE DE RASPUNDERE CIVILA AUTO RCA
                CARTE VERDE
                CUI 6291812
                Categoria vehiculului A
                Nr. inmatriculare CJ123GHY
                Valabilitate Contract de la 28-06-2025 pana la 27-06-2026
                """);

        assertThat(detection.documentType()).isEqualTo(DocumentType.INSURANCE);
        assertThat(detection.subtype()).isEqualTo("RCA");
    }

    @Test
    void doesNotTreatRarInsideAsigurariAsTechnicalInspection() {
        DocumentParsingService.DocumentTypeDetection detection = parsingService.detect("""
                MODEL EDITABIL
                GROUPAMA ASIGURARI S.A.
                CARTE VERDE
                Aceasta carte este valabila prin asigurare pentru prejudiciul cauzat.
                """);

        assertThat(detection.documentType()).isEqualTo(DocumentType.INSURANCE);
        assertThat(detection.subtype()).isEqualTo("RCA");
    }

    @Test
    void detectsStandaloneItpAndRarAsTechnicalInspection() {
        DocumentParsingService.DocumentTypeDetection detection = parsingService.detect("""
                Certificat ITP
                Inspectie tehnica periodica efectuata de statie autorizata RAR
                Valabilitate inspectie 2026-06-27
                """);

        assertThat(detection.documentType()).isEqualTo(DocumentType.TECHNICAL_INSPECTION);
        assertThat(detection.subtype()).isEqualTo("ITP");
    }

    @Test
    void textExtractionFailureReturnsFailedParserResult() {
        DocumentTextExtractionService textExtractionService = mock(DocumentTextExtractionService.class);
        DocumentParsingService service = new DocumentParsingService(
                textExtractionService,
                mock(DocumentLlmClient.class),
                new ObjectMapper(),
                "test-parser",
                "test"
        );
        VehicleDocument document = new VehicleDocument();
        document.setId(UUID.randomUUID());
        when(textExtractionService.extractText(document)).thenThrow(new TextExtractionException(
                "OCR_FAILED",
                "OCR failed while reading the PDF"
        ));

        ParserResultRequest result = service.parse(document);

        assertThat(result.parserStatus()).isEqualTo(ParserStatus.FAILED);
        assertThat(result.errorCode()).isEqualTo("OCR_FAILED");
    }

    @Test
    void parsedResultIncludesOpenAiUsage() {
        DocumentTextExtractionService textExtractionService = mock(DocumentTextExtractionService.class);
        DocumentLlmClient llmClient = mock(DocumentLlmClient.class);
        ObjectMapper objectMapper = new ObjectMapper();
        DocumentParsingService service = new DocumentParsingService(
                textExtractionService,
                llmClient,
                objectMapper,
                "test-parser",
                "test"
        );
        VehicleDocument document = new VehicleDocument();
        document.setId(UUID.randomUUID());
        when(textExtractionService.extractText(document)).thenReturn(new ExtractedTextResult(
                """
                        Asigurare RCA
                        Nr. polita RCA-123
                        Asigurator Test SA
                        Nr. inmatriculare B123ABC
                        Valabil pana la 2027-03-10
                """,
                TextExtractionMethod.PDFBOX,
                1,
                java.util.List.of(),
                0.9
        ));
        ObjectNode response = objectMapper.createObjectNode();
        response.put("policyNumber", "RCA-123");
        response.put("insurerName", "Test SA");
        response.put("licensePlate", "B 123 ABC");
        response.put("validUntil", "2027-03-10");
        response.put("llmConfidence", 0.8);
        LlmUsageDto usage = new LlmUsageDto("openai", "gpt-5.4-mini", "resp_123", 120, 60, 180);
        when(llmClient.extractJson(any(String.class), any(ObjectNode.class)))
                .thenReturn(new DocumentLlmClient.LlmExtractionResult(response, usage));

        ParserResultRequest result = service.parse(document);

        assertThat(result.parserStatus()).isEqualTo(ParserStatus.PARSED);
        assertThat(result.llmUsage()).isEqualTo(usage);
        assertThat(result.extractedData()).containsEntry("licensePlate", "B123ABC");
    }
}

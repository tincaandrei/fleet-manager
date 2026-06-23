package com.fleet.document.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleet.document.entity.DocumentType;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DocumentParsingServiceTest {

    private final DocumentParsingService parsingService = new DocumentParsingService(
            mock(DocumentStorageService.class),
            new ObjectMapper(),
            "http://localhost:11434",
            "llama3",
            Duration.ofSeconds(1),
            "test-parser",
            "test",
            20,
            200
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
}

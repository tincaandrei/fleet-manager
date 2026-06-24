package com.fleet.document.service.parsing;

import com.fleet.document.entity.TextExtractionMethod;
import com.fleet.document.entity.VehicleDocument;
import com.fleet.document.service.DocumentStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentTextExtractionServiceTest {

    @Mock
    private DocumentStorageService storageService;

    @Mock
    private PdfBoxTextExtractionService pdfBoxTextExtractionService;

    @Mock
    private OcrTextExtractionService ocrTextExtractionService;

    private DocumentTextExtractionService extractionService;

    @BeforeEach
    void setUp() {
        extractionService = new DocumentTextExtractionService(
                storageService,
                pdfBoxTextExtractionService,
                ocrTextExtractionService
        );
        ReflectionTestUtils.setField(extractionService, "minimumTextLength", 30);
        ReflectionTestUtils.setField(extractionService, "goodTextLength", 600);
    }

    @Test
    void usablePdfBoxTextDoesNotCallOcr() {
        VehicleDocument document = document();
        when(storageService.load(document.getStoragePath())).thenReturn(new ByteArrayResource(new byte[]{1, 2, 3}));
        when(pdfBoxTextExtractionService.extract(any(byte[].class))).thenReturn(new ExtractedTextResult(
                "Contract RCA asigurare valabilitate polita pentru vehicul B123ABC",
                TextExtractionMethod.PDFBOX,
                1,
                List.of(),
                0.0
        ));

        ExtractedTextResult result = extractionService.extractText(document);

        assertThat(result.extractionMethod()).isEqualTo(TextExtractionMethod.PDFBOX);
        assertThat(result.textQualityScore()).isGreaterThan(0.0);
        verifyNoInteractions(ocrTextExtractionService);
    }

    @Test
    void blankPdfBoxTextFallsBackToOcr() {
        VehicleDocument document = document();
        when(storageService.load(document.getStoragePath())).thenReturn(new ByteArrayResource(new byte[]{1, 2, 3}));
        when(pdfBoxTextExtractionService.extract(any(byte[].class))).thenReturn(new ExtractedTextResult(
                "",
                TextExtractionMethod.PDFBOX,
                1,
                List.of(),
                0.0
        ));
        when(ocrTextExtractionService.extract(any(byte[].class))).thenReturn(new ExtractedTextResult(
                "Factura pentru rovinieta CNADNR valabila pentru numar inmatriculare B123ABC",
                TextExtractionMethod.OCR,
                1,
                List.of(),
                0.0
        ));

        ExtractedTextResult result = extractionService.extractText(document);

        assertThat(result.extractionMethod()).isEqualTo(TextExtractionMethod.OCR);
        assertThat(result.warnings()).contains("OCR extraction may contain errors. Please verify manually.");
        verify(ocrTextExtractionService).extract(any(byte[].class));
    }

    @Test
    void imageDocumentUsesOcrDirectly() {
        VehicleDocument document = document();
        document.setOriginalFileName("rovinieta.jpg");
        document.setContentType("image/jpeg");
        when(storageService.load(document.getStoragePath())).thenReturn(new ByteArrayResource(new byte[]{1, 2, 3}));
        when(ocrTextExtractionService.extractImage(any(byte[].class))).thenReturn(new ExtractedTextResult(
                "Rovinieta CNAIR valabila pentru numar inmatriculare B123ABC",
                TextExtractionMethod.OCR,
                1,
                List.of(),
                0.0
        ));

        ExtractedTextResult result = extractionService.extractText(document);

        assertThat(result.extractionMethod()).isEqualTo(TextExtractionMethod.OCR);
        assertThat(result.warnings()).contains("OCR extraction may contain errors. Please verify manually.");
        verifyNoInteractions(pdfBoxTextExtractionService);
        verify(ocrTextExtractionService).extractImage(any(byte[].class));
    }

    @Test
    void corruptedPdfBoxTextFallsBackToOcr() {
        VehicleDocument document = document();
        when(storageService.load(document.getStoragePath())).thenReturn(new ByteArrayResource(new byte[]{1, 2, 3}));
        when(pdfBoxTextExtractionService.extract(any(byte[].class))).thenReturn(new ExtractedTextResult(
                "\uFFFD\uFFFD\uFFFD 1234 ???",
                TextExtractionMethod.PDFBOX,
                1,
                List.of(),
                0.0
        ));
        when(ocrTextExtractionService.extract(any(byte[].class))).thenReturn(new ExtractedTextResult(
                "Inspectie tehnica periodica ITP valabila pentru autovehicul B123ABC",
                TextExtractionMethod.OCR,
                1,
                List.of(),
                0.0
        ));

        ExtractedTextResult result = extractionService.extractText(document);

        assertThat(result.extractionMethod()).isEqualTo(TextExtractionMethod.OCR);
    }

    @Test
    void ocrFailureAfterUnusablePdfBoxTextFailsExtraction() {
        VehicleDocument document = document();
        when(storageService.load(document.getStoragePath())).thenReturn(new ByteArrayResource(new byte[]{1, 2, 3}));
        when(pdfBoxTextExtractionService.extract(any(byte[].class))).thenReturn(new ExtractedTextResult(
                "",
                TextExtractionMethod.PDFBOX,
                1,
                List.of(),
                0.0
        ));
        when(ocrTextExtractionService.extract(any(byte[].class))).thenThrow(new TextExtractionException(
                "OCR_FAILED",
                "OCR failed while reading the PDF"
        ));

        assertThatThrownBy(() -> extractionService.extractText(document))
                .isInstanceOf(TextExtractionException.class)
                .hasMessageContaining("OCR failed");
    }

    private VehicleDocument document() {
        VehicleDocument document = new VehicleDocument();
        document.setId(UUID.randomUUID());
        document.setOriginalFileName("document.pdf");
        document.setContentType("application/pdf");
        document.setStoragePath("/tmp/document.pdf");
        return document;
    }
}

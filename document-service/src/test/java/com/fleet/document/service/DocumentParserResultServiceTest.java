package com.fleet.document.service;

import com.fleet.document.dto.ParserResultRequest;
import com.fleet.document.entity.DocumentExtractionDraft;
import com.fleet.document.entity.DocumentStatus;
import com.fleet.document.entity.DocumentType;
import com.fleet.document.entity.ParserStatus;
import com.fleet.document.entity.VehicleDocument;
import com.fleet.document.repository.DocumentExtractionDraftRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentParserResultServiceTest {

    @Mock
    private DocumentExtractionDraftRepository extractionDraftRepository;

    @InjectMocks
    private DocumentParserResultService parserResultService;

    @Test
    void parsedResultCreatesDraftAndNeedsReviewStatus() {
        VehicleDocument document = document();
        when(extractionDraftRepository.findByDocument(document)).thenReturn(Optional.empty());
        when(extractionDraftRepository.save(any(DocumentExtractionDraft.class))).thenAnswer(invocation -> invocation.getArgument(0));

        parserResultService.applyParserResult(document, new ParserResultRequest(
                document.getId(),
                ParserStatus.PARSED,
                "INSURANCE",
                "RCA",
                new BigDecimal("0.91"),
                "test-parser",
                "1.0",
                Map.of("licensePlate", "B123ABC", "validUntil", "2027-03-10"),
                java.util.List.of(),
                null,
                null
        ));

        ArgumentCaptor<DocumentExtractionDraft> draftCaptor = ArgumentCaptor.forClass(DocumentExtractionDraft.class);
        verify(extractionDraftRepository).save(draftCaptor.capture());
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.NEEDS_REVIEW);
        assertThat(document.getDocumentType()).isEqualTo(DocumentType.INSURANCE);
        assertThat(document.getDocumentSubtype()).isEqualTo("RCA");
        assertThat(draftCaptor.getValue().getParserStatus()).isEqualTo(ParserStatus.PARSED);
        assertThat(draftCaptor.getValue().getExtractedData()).containsEntry("licensePlate", "B123ABC");
    }

    @Test
    void failedResultSetsParsingFailedStatus() {
        VehicleDocument document = document();
        when(extractionDraftRepository.findByDocument(document)).thenReturn(Optional.empty());
        when(extractionDraftRepository.save(any(DocumentExtractionDraft.class))).thenAnswer(invocation -> invocation.getArgument(0));

        parserResultService.applyParserResult(document, new ParserResultRequest(
                document.getId(),
                ParserStatus.FAILED,
                null,
                null,
                null,
                "test-parser",
                "1.0",
                null,
                null,
                "TEXT_EXTRACTION_FAILED",
                "Could not extract readable text from PDF"
        ));

        ArgumentCaptor<DocumentExtractionDraft> draftCaptor = ArgumentCaptor.forClass(DocumentExtractionDraft.class);
        verify(extractionDraftRepository).save(draftCaptor.capture());
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.PARSING_FAILED);
        assertThat(draftCaptor.getValue().getParserStatus()).isEqualTo(ParserStatus.FAILED);
        assertThat(draftCaptor.getValue().getErrorCode()).isEqualTo("TEXT_EXTRACTION_FAILED");
    }

    private VehicleDocument document() {
        VehicleDocument document = new VehicleDocument();
        document.setId(UUID.randomUUID());
        document.setVehicleId(1L);
        document.setBusinessId(100L);
        document.setDocumentType(DocumentType.OTHER);
        document.setStatus(DocumentStatus.PARSING);
        document.setOriginalFileName("document.pdf");
        document.setStoredFileName("stored.pdf");
        document.setContentType("application/pdf");
        document.setFileSize(123L);
        document.setStoragePath("/tmp/stored.pdf");
        document.setUploadedByUserId(10L);
        return document;
    }
}

package com.fleet.document.service;

import com.fleet.document.dto.ParserResultRequest;
import com.fleet.document.entity.DocumentStatus;
import com.fleet.document.entity.DocumentType;
import com.fleet.document.entity.ParserStatus;
import com.fleet.document.entity.TextExtractionMethod;
import com.fleet.document.entity.VehicleDocument;
import com.fleet.document.repository.VehicleDocumentRepository;
import com.fleet.document.messaging.DocumentParsingMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentParsingConsumerTest {

    @Mock
    private VehicleDocumentRepository documentRepository;

    @Mock
    private DocumentParsingService documentParsingService;

    @Mock
    private DocumentParserResultService parserResultService;

    @Mock
    private UserNotificationService notificationService;

    @InjectMocks
    private DocumentParsingConsumer consumer;

    @Test
    void successfulParsingSavesNeedsReviewAndNotifiesUploader() {
        VehicleDocument document = document();
        ParserResultRequest result = parsedResult(document.getId());
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(documentParsingService.parse(document)).thenReturn(result);
        doAnswer(invocation -> {
            VehicleDocument parsedDocument = invocation.getArgument(0);
            parsedDocument.setStatus(DocumentStatus.NEEDS_REVIEW);
            return null;
        }).when(parserResultService).applyParserResult(document, result);

        consumer.consume(new DocumentParsingMessage(document.getId(), "B123ABC", java.util.List.of(11L)));

        verify(parserResultService).applyParserResult(document, result);
        verify(documentRepository).save(document);
        verify(notificationService).notifyParsingCompleted(10L, document.getId(), false);
        verify(notificationService).notifyDocumentPendingReview(11L, document.getId(), "B123ABC");
    }

    @Test
    void ocrParsingCompletionNotifiesUploaderWithOcrFlag() {
        VehicleDocument document = document();
        ParserResultRequest result = parsedResult(document.getId(), TextExtractionMethod.OCR);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(documentParsingService.parse(document)).thenReturn(result);
        doAnswer(invocation -> {
            VehicleDocument parsedDocument = invocation.getArgument(0);
            parsedDocument.setStatus(DocumentStatus.NEEDS_REVIEW);
            return null;
        }).when(parserResultService).applyParserResult(document, result);
        consumer.consume(new DocumentParsingMessage(document.getId(), "B123ABC", java.util.List.of()));

        verify(notificationService).notifyParsingCompleted(10L, document.getId(), true);
    }

    @Test
    void adminNotificationSkipsUploaderWhenUploaderIsBusinessAdmin() {
        VehicleDocument document = document();
        ParserResultRequest result = parsedResult(document.getId());
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(documentParsingService.parse(document)).thenReturn(result);
        doAnswer(invocation -> {
            VehicleDocument parsedDocument = invocation.getArgument(0);
            parsedDocument.setStatus(DocumentStatus.NEEDS_REVIEW);
            return null;
        }).when(parserResultService).applyParserResult(document, result);
        consumer.consume(new DocumentParsingMessage(document.getId(), "B123ABC", java.util.List.of()));

        verify(notificationService).notifyParsingCompleted(10L, document.getId(), false);
        verify(notificationService, org.mockito.Mockito.never())
                .notifyDocumentPendingReview(any(), any(), any());
    }

    @Test
    void failedParsingSavesParsingFailedAndNotifiesUploader() {
        VehicleDocument document = document();
        ParserResultRequest result = new ParserResultRequest(
                document.getId(),
                ParserStatus.FAILED,
                null,
                null,
                null,
                "test-parser",
                "1.0",
                null,
                null,
                null,
                null,
                "TEXT_EXTRACTION_FAILED",
                "Could not extract readable text from PDF"
        );
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(documentParsingService.parse(document)).thenReturn(result);
        doAnswer(invocation -> {
            VehicleDocument failedDocument = invocation.getArgument(0);
            failedDocument.setStatus(DocumentStatus.PARSING_FAILED);
            return null;
        }).when(parserResultService).applyParserResult(document, result);

        consumer.consume(new DocumentParsingMessage(document.getId(), "B123ABC", java.util.List.of()));

        verify(parserResultService).applyParserResult(document, result);
        verify(documentRepository).save(document);
        verify(notificationService).notifyParsingFailed(10L, document.getId());
    }

    @Test
    void duplicateMessageForAlreadyProcessedDocumentIsIgnored() {
        VehicleDocument document = document();
        document.setStatus(DocumentStatus.NEEDS_REVIEW);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));

        consumer.consume(new DocumentParsingMessage(document.getId(), "B123ABC", java.util.List.of(11L)));

        verifyNoInteractions(documentParsingService, parserResultService, notificationService);
    }

    private ParserResultRequest parsedResult(UUID documentId) {
        return parsedResult(documentId, TextExtractionMethod.PDFBOX);
    }

    private ParserResultRequest parsedResult(UUID documentId, TextExtractionMethod extractionMethod) {
        return new ParserResultRequest(
                documentId,
                ParserStatus.PARSED,
                "INSURANCE",
                "RCA",
                new BigDecimal("0.91"),
                "test-parser",
                "1.0",
                extractionMethod,
                null,
                Map.of("licensePlate", "B123ABC"),
                java.util.List.of(),
                null,
                null
        );
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

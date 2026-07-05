package com.fleet.document.service;

import com.fleet.document.dto.ParserResultRequest;
import com.fleet.document.entity.DocumentStatus;
import com.fleet.document.entity.ParserStatus;
import com.fleet.document.entity.TextExtractionMethod;
import com.fleet.document.entity.VehicleDocument;
import com.fleet.document.repository.VehicleDocumentRepository;
import com.fleet.document.service.event.DocumentUploadedForParsingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentParsingJobListener {

    private final VehicleDocumentRepository documentRepository;
    private final DocumentParsingService documentParsingService;
    private final DocumentParserResultService parserResultService;
    private final UserNotificationService notificationService;
    private final AuthUserLookupClient authUserLookupClient;

    @Async("documentParsingExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void parseUploadedDocument(DocumentUploadedForParsingEvent event) {
        documentRepository.findById(event.documentId()).ifPresentOrElse(document -> parseDocument(document, event), () ->
                log.warn("Skipping parser job for missing document {}", event.documentId()));
    }

    private void parseDocument(VehicleDocument document, DocumentUploadedForParsingEvent event) {
        if (document.getStatus() != DocumentStatus.PARSING) {
            log.info("Skipping parser job for document {} with status {}", document.getId(), document.getStatus());
            return;
        }

        ParserResultRequest parserResult;
        try {
            parserResult = documentParsingService.parse(document);
        } catch (RuntimeException ex) {
            log.warn("Document parser request failed for document {}", document.getId(), ex);
            parserResult = new ParserResultRequest(
                    document.getId(),
                    ParserStatus.FAILED,
                    null,
                    null,
                    null,
                    "document-service-openai-parser",
                    null,
                    null,
                    null,
                    null,
                    List.of("Automatic parsing failed. The document can be reviewed manually."),
                    "PARSER_REQUEST_FAILED",
                    "Could not parse document"
            );
        }

        parserResultService.applyParserResult(document, parserResult);
        documentRepository.save(document);
        createUploaderNotification(document, parserResult);
        createAdminPendingReviewNotifications(document, event);
    }

    private void createUploaderNotification(VehicleDocument document, ParserResultRequest parserResult) {
        if (document.getStatus() == DocumentStatus.NEEDS_REVIEW) {
            notificationService.notifyParsingCompleted(
                    document.getUploadedByUserId(),
                    document.getId(),
                    parserResult.extractionMethod() == TextExtractionMethod.OCR
            );
        } else if (document.getStatus() == DocumentStatus.PARSING_FAILED) {
            notificationService.notifyParsingFailed(document.getUploadedByUserId(), document.getId());
        }
    }

    private void createAdminPendingReviewNotifications(VehicleDocument document, DocumentUploadedForParsingEvent event) {
        if (document.getStatus() != DocumentStatus.NEEDS_REVIEW) {
            return;
        }

        authUserLookupClient.lookupBusinessAdmins(document.getBusinessId(), event.authorizationHeader()).stream()
                .map(com.fleet.document.dto.UserLookupResponse::userId)
                .filter(userId -> userId != null && !userId.equals(document.getUploadedByUserId()))
                .distinct()
                .forEach(userId -> notificationService.notifyDocumentPendingReview(
                        userId,
                        document.getId(),
                        event.vehicleLabel()
                ));
    }
}

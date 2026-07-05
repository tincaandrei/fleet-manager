package com.fleet.document.service;

import com.fleet.document.dto.ParserResultRequest;
import com.fleet.document.entity.DocumentStatus;
import com.fleet.document.entity.ParserStatus;
import com.fleet.document.entity.TextExtractionMethod;
import com.fleet.document.entity.VehicleDocument;
import com.fleet.document.messaging.DocumentParsingMessage;
import com.fleet.document.repository.VehicleDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DocumentParsingConsumer {

    private final VehicleDocumentRepository documentRepository;
    private final DocumentParsingService documentParsingService;
    private final DocumentParserResultService parserResultService;
    private final UserNotificationService notificationService;

    @RabbitListener(queues = "${app.rabbitmq.parsing-queue:doccufleet.document.parsing}")
    @Transactional
    public void consume(DocumentParsingMessage message) {
        documentRepository.findById(message.documentId()).ifPresentOrElse(
                document -> parseDocument(document, message),
                () -> log.warn("Skipping parser message for missing document {}", message.documentId())
        );
    }

    private void parseDocument(VehicleDocument document, DocumentParsingMessage message) {
        if (document.getStatus() != DocumentStatus.PARSING) {
            log.info("Skipping duplicate parser message for document {} with status {}",
                    document.getId(), document.getStatus());
            return;
        }

        ParserResultRequest parserResult;
        try {
            parserResult = documentParsingService.parse(document);
        } catch (RuntimeException exception) {
            log.warn("Document parser request failed for document {}", document.getId(), exception);
            parserResult = failedParserResult(document);
        }

        parserResultService.applyParserResult(document, parserResult);
        documentRepository.save(document);
        createUploaderNotification(document, parserResult);
        createAdminPendingReviewNotifications(document, message);
    }

    private ParserResultRequest failedParserResult(VehicleDocument document) {
        return new ParserResultRequest(
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

    private void createAdminPendingReviewNotifications(
            VehicleDocument document,
            DocumentParsingMessage message
    ) {
        if (document.getStatus() != DocumentStatus.NEEDS_REVIEW) {
            return;
        }
        message.adminUserIds().forEach(userId -> notificationService.notifyDocumentPendingReview(
                userId,
                document.getId(),
                message.vehicleLabel()
        ));
    }
}

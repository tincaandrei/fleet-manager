package com.fleet.document.service;

import com.fleet.document.dto.MarkAllNotificationsReadResponse;
import com.fleet.document.dto.NotificationResponse;
import com.fleet.document.dto.UnreadNotificationCountResponse;
import com.fleet.document.entity.NotificationType;
import com.fleet.document.entity.UserNotification;
import com.fleet.document.exception.ResourceNotFoundException;
import com.fleet.document.repository.UserNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserNotificationService {

    private static final String PARSING_COMPLETED_TITLE = "Document parsing completed";
    private static final String PARSING_COMPLETED_MESSAGE = "Your uploaded document has been processed and is ready for review.";
    private static final String OCR_PARSING_COMPLETED_MESSAGE = "Your uploaded document has been processed and is ready for review. The document was scanned, so OCR was used. Please verify the extracted fields.";
    private static final String PARSING_FAILED_TITLE = "Document parsing failed";
    private static final String PARSING_FAILED_MESSAGE = "We could not automatically process your document. Please review or reupload it.";
    private static final String PENDING_REVIEW_TITLE = "Document pending review";
    private static final String REPORT_GENERATED_TITLE = "Report generated";
    private static final String DOCUMENT_EXPIRING_TITLE = "Document expiring soon";

    private final UserNotificationRepository notificationRepository;

    @Transactional
    public void notifyParsingCompleted(Long userId, UUID documentId) {
        notifyParsingCompleted(userId, documentId, false);
    }

    @Transactional
    public void notifyParsingCompleted(Long userId, UUID documentId, boolean ocrUsed) {
        createDocumentNotification(
                userId,
                documentId,
                NotificationType.DOCUMENT_PARSING_COMPLETED,
                PARSING_COMPLETED_TITLE,
                ocrUsed ? OCR_PARSING_COMPLETED_MESSAGE : PARSING_COMPLETED_MESSAGE
        );
    }

    @Transactional
    public void notifyParsingFailed(Long userId, UUID documentId) {
        createDocumentNotification(
                userId,
                documentId,
                NotificationType.DOCUMENT_PARSING_FAILED,
                PARSING_FAILED_TITLE,
                PARSING_FAILED_MESSAGE
        );
    }

    @Transactional
    public void notifyDocumentPendingReview(Long userId, UUID documentId, String vehicleLabel) {
        String label = vehicleLabel == null || vehicleLabel.isBlank() ? "the selected vehicle" : vehicleLabel.trim();
        createDocumentNotification(
                userId,
                documentId,
                NotificationType.DOCUMENT_PENDING_REVIEW,
                PENDING_REVIEW_TITLE,
                "For vehicle " + label + " there is a document pending review."
        );
    }

    @Transactional
    public void notifyReportGenerated(Long userId, String reportName, String scopeLabel) {
        String scope = scopeLabel == null || scopeLabel.isBlank() ? "your organization" : scopeLabel.trim();
        createDocumentNotification(
                userId,
                null,
                NotificationType.REPORT_GENERATED,
                REPORT_GENERATED_TITLE,
                reportName + " has been generated for " + scope + "."
        );
    }

    @Transactional
    public void notifyDocumentExpiring(
            Long userId,
            UUID documentId,
            String vehicleLabel,
            String documentLabel,
            LocalDate validUntil,
            long daysLeft
    ) {
        String vehicle = vehicleLabel == null || vehicleLabel.isBlank() ? "a vehicle" : vehicleLabel.trim();
        String document = documentLabel == null || documentLabel.isBlank() ? "A document" : documentLabel.trim();
        String deadline = daysLeft <= 0
                ? "expires today"
                : "expires on " + validUntil + " (" + daysLeft + (daysLeft == 1 ? " day" : " days") + " left)";
        createDocumentNotification(
                userId,
                documentId,
                NotificationType.DOCUMENT_EXPIRING,
                DOCUMENT_EXPIRING_TITLE,
                document + " for vehicle " + vehicle + " " + deadline + "."
        );
    }

    @Transactional(readOnly = true)
    public boolean hasNotification(Long userId, UUID documentId, NotificationType type) {
        return notificationRepository.existsByUserIdAndDocumentIdAndType(userId, documentId, type);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listForCurrentUser(Authentication authentication) {
        Long userId = requireCurrentUserId(authentication);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse unreadCount(Authentication authentication) {
        Long userId = requireCurrentUserId(authentication);
        return new UnreadNotificationCountResponse(notificationRepository.countByUserIdAndReadFalse(userId));
    }

    @Transactional
    public NotificationResponse markRead(UUID id, Authentication authentication) {
        Long userId = requireCurrentUserId(authentication);
        UserNotification notification = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));
        notification.setRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public MarkAllNotificationsReadResponse markAllRead(Authentication authentication) {
        Long userId = requireCurrentUserId(authentication);
        List<UserNotification> unreadNotifications = notificationRepository.findByUserIdAndReadFalse(userId);
        unreadNotifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unreadNotifications);
        return new MarkAllNotificationsReadResponse(unreadNotifications.size());
    }

    private void createDocumentNotification(
            Long userId,
            UUID documentId,
            NotificationType type,
            String title,
            String message
    ) {
        if (userId == null) {
            return;
        }
        notificationRepository.save(UserNotification.builder()
                .userId(userId)
                .documentId(documentId)
                .type(type)
                .title(title)
                .message(message)
                .read(false)
                .build());
    }

    private Long requireCurrentUserId(Authentication authentication) {
        Long userId = SecurityUtils.currentUserId(authentication);
        if (userId == null) {
            throw new AccessDeniedException("Access denied");
        }
        return userId;
    }

    private NotificationResponse toResponse(UserNotification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getDocumentId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}

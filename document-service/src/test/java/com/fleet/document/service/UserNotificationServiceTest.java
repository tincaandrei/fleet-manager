package com.fleet.document.service;

import com.fleet.document.entity.NotificationType;
import com.fleet.document.entity.UserNotification;
import com.fleet.document.exception.ResourceNotFoundException;
import com.fleet.document.repository.UserNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserNotificationServiceTest {

    @Mock
    private UserNotificationRepository notificationRepository;

    @InjectMocks
    private UserNotificationService notificationService;

    private final Authentication authentication = new TestingAuthenticationToken(
            new JwtPrincipal("user", 10L, 100L, Set.of("EMPLOYEE")),
            null,
            "ROLE_EMPLOYEE"
    );

    @Test
    void createsParsingCompletedNotificationForUploader() {
        when(notificationRepository.save(any(UserNotification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UUID documentId = UUID.randomUUID();

        notificationService.notifyParsingCompleted(10L, documentId);

        verify(notificationRepository).save(org.mockito.ArgumentMatchers.argThat(notification ->
                notification.getUserId().equals(10L)
                        && notification.getDocumentId().equals(documentId)
                        && notification.getType() == NotificationType.DOCUMENT_PARSING_COMPLETED
                        && !notification.isRead()
        ));
    }

    @Test
    void createsOcrParsingCompletedNotificationForUploader() {
        when(notificationRepository.save(any(UserNotification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UUID documentId = UUID.randomUUID();

        notificationService.notifyParsingCompleted(10L, documentId, true);

        verify(notificationRepository).save(org.mockito.ArgumentMatchers.argThat(notification ->
                notification.getUserId().equals(10L)
                        && notification.getDocumentId().equals(documentId)
                        && notification.getType() == NotificationType.DOCUMENT_PARSING_COMPLETED
                        && notification.getMessage().contains("OCR was used")
        ));
    }

    @Test
    void listsOnlyCurrentUserNotifications() {
        UserNotification notification = notification(UUID.randomUUID(), 10L, false);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(notification));

        assertThat(notificationService.listForCurrentUser(authentication))
                .singleElement()
                .satisfies(response -> {
                    assertThat(response.id()).isEqualTo(notification.getId());
                    assertThat(response.userId()).isEqualTo(10L);
                    assertThat(response.isRead()).isFalse();
                });
    }

    @Test
    void returnsUnreadCountForCurrentUser() {
        when(notificationRepository.countByUserIdAndReadFalse(10L)).thenReturn(2L);

        assertThat(notificationService.unreadCount(authentication).count()).isEqualTo(2L);
    }

    @Test
    void markReadRejectsOtherUsersNotification() {
        UUID notificationId = UUID.randomUUID();
        when(notificationRepository.findByIdAndUserId(notificationId, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markRead(notificationId, authentication))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private UserNotification notification(UUID id, Long userId, boolean read) {
        UserNotification notification = new UserNotification();
        notification.setId(id);
        notification.setUserId(userId);
        notification.setDocumentId(UUID.randomUUID());
        notification.setType(NotificationType.DOCUMENT_PARSING_COMPLETED);
        notification.setTitle("Document parsing completed");
        notification.setMessage("Your uploaded document has been processed and is ready for review.");
        notification.setRead(read);
        notification.setCreatedAt(Instant.now());
        return notification;
    }
}

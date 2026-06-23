package com.fleet.document.dto;

import com.fleet.document.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        Long userId,
        UUID documentId,
        NotificationType type,
        String title,
        String message,
        boolean isRead,
        Instant createdAt
) {
}

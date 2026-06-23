package com.fleet.document.controller;

import com.fleet.document.dto.MarkAllNotificationsReadResponse;
import com.fleet.document.dto.NotificationResponse;
import com.fleet.document.dto.UnreadNotificationCountResponse;
import com.fleet.document.service.UserNotificationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notifications", description = "In-app user notifications.")
@RequestMapping("/notifications")
public class NotificationController {

    private final UserNotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list(Authentication authentication) {
        return ResponseEntity.ok(notificationService.listForCurrentUser(authentication));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadNotificationCountResponse> unreadCount(Authentication authentication) {
        return ResponseEntity.ok(notificationService.unreadCount(authentication));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(notificationService.markRead(id, authentication));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<MarkAllNotificationsReadResponse> markAllRead(Authentication authentication) {
        return ResponseEntity.ok(notificationService.markAllRead(authentication));
    }
}

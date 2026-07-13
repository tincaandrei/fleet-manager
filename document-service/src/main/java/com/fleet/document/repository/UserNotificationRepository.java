package com.fleet.document.repository;

import com.fleet.document.entity.NotificationType;
import com.fleet.document.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {

    List<UserNotification> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<UserNotification> findByIdAndUserId(UUID id, Long userId);

    long countByUserIdAndReadFalse(Long userId);

    List<UserNotification> findByUserIdAndReadFalse(Long userId);

    boolean existsByUserIdAndDocumentIdAndType(Long userId, UUID documentId, NotificationType type);
}

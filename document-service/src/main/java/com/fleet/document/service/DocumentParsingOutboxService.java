package com.fleet.document.service;

import com.fleet.document.entity.DocumentParsingOutboxEvent;
import com.fleet.document.messaging.DocumentParsingMessage;
import com.fleet.document.repository.DocumentParsingOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentParsingOutboxService {

    private final DocumentParsingOutboxRepository outboxRepository;

    public void enqueue(DocumentParsingMessage message) {
        outboxRepository.save(DocumentParsingOutboxEvent.builder()
                .documentId(message.documentId())
                .vehicleLabel(message.vehicleLabel())
                .adminUserIds(serializeUserIds(message.adminUserIds()))
                .build());
    }

    public DocumentParsingMessage toMessage(DocumentParsingOutboxEvent event) {
        return new DocumentParsingMessage(
                event.getDocumentId(),
                event.getVehicleLabel(),
                deserializeUserIds(event.getAdminUserIds())
        );
    }

    private String serializeUserIds(List<Long> userIds) {
        return userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private List<Long> deserializeUserIds(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .map(Long::valueOf)
                .toList();
    }
}

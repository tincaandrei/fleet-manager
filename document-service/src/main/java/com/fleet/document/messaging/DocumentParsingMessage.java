package com.fleet.document.messaging;

import java.util.List;
import java.util.UUID;

public record DocumentParsingMessage(
        UUID documentId,
        String vehicleLabel,
        List<Long> adminUserIds
) {
    public DocumentParsingMessage {
        adminUserIds = adminUserIds == null ? List.of() : List.copyOf(adminUserIds);
    }
}

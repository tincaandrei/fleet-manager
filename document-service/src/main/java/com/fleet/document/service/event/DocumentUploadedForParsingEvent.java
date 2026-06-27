package com.fleet.document.service.event;

import java.util.UUID;

public record DocumentUploadedForParsingEvent(
        UUID documentId,
        String authorizationHeader,
        String vehicleLabel
) {
}

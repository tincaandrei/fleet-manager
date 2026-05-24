package com.fleet.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

@Schema(description = "Official reviewed vehicle document data.")
public record ApprovedDocumentDataResponse(
        String documentType,
        String subtype,
        LocalDate validFrom,
        LocalDate validUntil,
        Map<String, Object> approvedData,
        Long approvedBy,
        Instant approvedAt
) {
}

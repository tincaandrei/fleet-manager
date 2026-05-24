package com.fleet.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.Map;

@Schema(description = "Staff/admin approval payload for official document data.")
public record ApproveDocumentRequest(
        @Schema(description = "Approved official vehicle document data.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "Approved data is required")
        Map<String, Object> approvedData,

        @Schema(description = "Official document type.", example = "INSPECTION", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Document type is required")
        String documentType,

        @Schema(description = "Optional subtype.", example = "PERIODIC_TECHNICAL_INSPECTION")
        String subtype,

        LocalDate validFrom,
        LocalDate validUntil
) {
}

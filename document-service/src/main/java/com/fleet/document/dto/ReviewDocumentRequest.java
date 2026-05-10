package com.fleet.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

@Schema(description = "Payload used by staff/admin to approve or reject extracted data.")
public record ReviewDocumentRequest(
        @Schema(description = "Review decision.", example = "APPROVE", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Decision is required")
        ReviewDecision decision,

        @Schema(description = "Approved document data. Required when decision is APPROVE.")
        Map<String, Object> approvedData,

        @Schema(description = "Optional review comment.", example = "Approved after correcting mileage.")
        String comment
) {
}

package com.fleet.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Staff/admin rejection payload.")
public record RejectDocumentRequest(
        @Schema(description = "Reason for rejecting the extracted data.", example = "Document is unreadable.")
        String reason
) {
}

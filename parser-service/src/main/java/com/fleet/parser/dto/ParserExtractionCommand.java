package com.fleet.parser.dto;

import org.springframework.web.multipart.MultipartFile;

public record ParserExtractionCommand(
        MultipartFile file,
        String documentId,
        String vehicleId,
        String declaredDocumentType,
        String originalFileName
) {
}

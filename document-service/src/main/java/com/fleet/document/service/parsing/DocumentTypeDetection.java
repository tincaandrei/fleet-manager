package com.fleet.document.service.parsing;

import com.fleet.document.entity.DocumentType;

import java.util.List;

public record DocumentTypeDetection(
        DocumentType documentType,
        String subtype,
        double confidence,
        List<String> matchedKeywords
) {
}

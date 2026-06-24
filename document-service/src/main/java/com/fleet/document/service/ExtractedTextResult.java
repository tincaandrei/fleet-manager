package com.fleet.document.service;

import com.fleet.document.entity.TextExtractionMethod;

import java.util.List;

public record ExtractedTextResult(
        String text,
        TextExtractionMethod extractionMethod,
        int pageCount,
        List<String> warnings,
        double textQualityScore
) {
}

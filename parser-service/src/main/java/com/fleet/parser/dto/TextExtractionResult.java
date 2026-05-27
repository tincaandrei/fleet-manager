package com.fleet.parser.dto;

public record TextExtractionResult(
        String text,
        ExtractionMethod extractionMethod,
        double textQualityScore
) {
}

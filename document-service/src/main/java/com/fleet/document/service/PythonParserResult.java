package com.fleet.document.service;

import java.math.BigDecimal;
import java.util.Map;

public record PythonParserResult(
        boolean parsed,
        String parserName,
        String parserVersion,
        BigDecimal confidence,
        Map<String, Object> data,
        String errorMessage
) {
}

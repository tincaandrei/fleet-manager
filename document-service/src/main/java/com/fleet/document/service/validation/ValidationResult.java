package com.fleet.document.service.validation;

import java.util.List;

public record ValidationResult(
        List<String> warnings,
        double validationScore
) {
}

package com.fleet.document.service.normalization;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AmountNormalizer {

    public BigDecimal normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim()
                .replace(" ", "")
                .replace(",", ".");
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}

package com.fleet.document.service.normalization;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class LicensePlateNormalizer {

    private static final Pattern ROMANIAN_LICENSE_PLATE_PATTERN = Pattern.compile("^(?:B\\d{2,3}[A-Z]{3}|[A-Z]{2}\\d{2,3}[A-Z]{3})$");

    public String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    public boolean isValidRomanianPlate(String value) {
        return value != null && ROMANIAN_LICENSE_PLATE_PATTERN.matcher(value).matches();
    }
}

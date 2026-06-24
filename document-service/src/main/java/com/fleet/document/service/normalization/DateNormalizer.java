package com.fleet.document.service.normalization;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Component
public class DateNormalizer {

    private static final List<DateTimeFormatter> INPUT_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
    );

    public String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 10 && isIsoDate(trimmed.substring(0, 10))) {
            return trimmed.substring(0, 10);
        }
        for (DateTimeFormatter formatter : INPUT_FORMATTERS) {
            try {
                return LocalDate.parse(trimmed, formatter).format(DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException ignored) {
                // Try the next accepted date format.
            }
        }
        return null;
    }

    public boolean isIsoDate(String value) {
        try {
            LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
            return true;
        } catch (DateTimeParseException exception) {
            return false;
        }
    }
}

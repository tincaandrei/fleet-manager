package com.fleet.document.service.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fleet.document.entity.TextExtractionMethod;
import com.fleet.document.service.normalization.DateNormalizer;
import com.fleet.document.service.normalization.LicensePlateNormalizer;
import com.fleet.document.service.normalization.VinNormalizer;
import com.fleet.document.service.parsing.ExtractedTextResult;
import com.fleet.document.service.schema.DocumentExtractionSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentExtractionValidator {

    private final LicensePlateNormalizer licensePlateNormalizer;
    private final VinNormalizer vinNormalizer;
    private final DateNormalizer dateNormalizer;

    public ValidationResult validate(JsonNode extractedData, DocumentExtractionSchema schema, ExtractedTextResult textResult) {
        List<String> warnings = new ArrayList<>();

        for (String field : schema.importantFields()) {
            if (!hasValue(extractedData, field)) {
                warnings.add("Missing important field: " + field);
            }
        }

        if (schema.requiresVehicleIdentifier() && !hasValue(extractedData, "licensePlate") && !hasValue(extractedData, "vin")) {
            warnings.add("Missing both licensePlate and vin");
        }

        if (hasValue(extractedData, "licensePlate")
                && !licensePlateNormalizer.isValidRomanianPlate(extractedData.path("licensePlate").asText())) {
            warnings.add("License plate appears malformed");
        }

        if (hasValue(extractedData, "vin") && !vinNormalizer.isValidVin(extractedData.path("vin").asText())) {
            warnings.add("VIN appears malformed");
        }

        for (String field : List.of("validFrom", "validUntil", "inspectionDate", "invoiceDate", "issueDate")) {
            if (hasValue(extractedData, field) && !dateNormalizer.isIsoDate(extractedData.path(field).asText())) {
                warnings.add("Invalid date format for " + field);
            }
        }

        if (textResult != null && textResult.extractionMethod() == TextExtractionMethod.OCR) {
            warnings.add("OCR extraction may contain errors. Please verify manually.");
        }

        return new ValidationResult(warnings, Math.max(0.0, 1.0 - (warnings.size() * 0.15)));
    }

    private boolean hasValue(JsonNode data, String field) {
        if (data == null || data.isNull()) {
            return false;
        }
        JsonNode node = data.path(field);
        return !node.isMissingNode() && !node.isNull() && (!node.isTextual() || !node.asText().isBlank());
    }
}

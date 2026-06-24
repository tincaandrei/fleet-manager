package com.fleet.document.service.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fleet.document.entity.TextExtractionMethod;
import com.fleet.document.service.normalization.DateNormalizer;
import com.fleet.document.service.normalization.LicensePlateNormalizer;
import com.fleet.document.service.normalization.VinNormalizer;
import com.fleet.document.service.parsing.ExtractedTextResult;
import com.fleet.document.service.schema.RcaExtractionSchema;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentExtractionValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DocumentExtractionValidator validator = new DocumentExtractionValidator(
            new LicensePlateNormalizer(),
            new VinNormalizer(),
            new DateNormalizer()
    );

    @Test
    void warnsWhenVehicleIdentifierIsMissing() {
        ObjectNode data = objectMapper.createObjectNode();

        ValidationResult result = validator.validate(data, new RcaExtractionSchema(), textResult(TextExtractionMethod.PDFBOX));

        assertThat(result.warnings()).contains("Missing both licensePlate and vin");
    }

    @Test
    void warnsForMalformedPlateAndVinAndOcr() {
        ObjectNode data = objectMapper.createObjectNode();
        data.put("licensePlate", "ABC");
        data.put("vin", "INVALID");

        ValidationResult result = validator.validate(data, new RcaExtractionSchema(), textResult(TextExtractionMethod.OCR));

        assertThat(result.warnings()).contains(
                "License plate appears malformed",
                "VIN appears malformed",
                "OCR extraction may contain errors. Please verify manually."
        );
    }

    private ExtractedTextResult textResult(TextExtractionMethod method) {
        return new ExtractedTextResult("text", method, 1, List.of(), 0.8);
    }
}

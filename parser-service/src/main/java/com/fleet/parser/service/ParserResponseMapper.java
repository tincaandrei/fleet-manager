package com.fleet.parser.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fleet.parser.config.ParserProperties;
import com.fleet.parser.dto.DocumentSubtype;
import com.fleet.parser.dto.DocumentType;
import com.fleet.parser.dto.ExtractionMethod;
import com.fleet.parser.dto.ParserErrorCode;
import com.fleet.parser.dto.ParserExtractionResponse;
import com.fleet.parser.dto.ParserStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ParserResponseMapper {

    private final ParserProperties properties;

    public ParserResponseMapper(ParserProperties properties) {
        this.properties = properties;
    }

    public ParserExtractionResponse success(String documentId,
                                            DocumentType documentType,
                                            DocumentSubtype subtype,
                                            double confidence,
                                            ExtractionMethod extractionMethod,
                                            JsonNode extractedData,
                                            List<String> warnings) {
        return new ParserExtractionResponse(
                documentId,
                ParserStatus.PARSED,
                documentType,
                subtype,
                confidence,
                properties.getName(),
                properties.getVersion(),
                extractionMethod,
                extractedData,
                warnings == null ? List.of() : List.copyOf(warnings),
                null,
                null
        );
    }

    public ParserExtractionResponse failure(String documentId,
                                            ParserErrorCode errorCode,
                                            String errorMessage,
                                            List<String> warnings) {
        List<String> responseWarnings = new ArrayList<>();
        if (warnings != null) {
            responseWarnings.addAll(warnings);
        }
        if (responseWarnings.isEmpty() && errorMessage != null) {
            responseWarnings.add(errorMessage);
        }

        return new ParserExtractionResponse(
                documentId,
                ParserStatus.FAILED,
                null,
                null,
                0.0,
                properties.getName(),
                properties.getVersion(),
                null,
                null,
                responseWarnings,
                errorCode,
                errorMessage
        );
    }
}

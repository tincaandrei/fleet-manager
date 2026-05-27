package com.fleet.parser.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleet.parser.dto.DocumentSubtype;
import com.fleet.parser.dto.DocumentType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;

@Component
public class GenericExtractionStrategy extends AbstractDocumentExtractionStrategy {

    public GenericExtractionStrategy(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public boolean supports(DocumentType type, DocumentSubtype subtype) {
        return type == DocumentType.OTHER;
    }

    @Override
    public DocumentType documentType() {
        return DocumentType.OTHER;
    }

    @Override
    public DocumentSubtype subtype() {
        return DocumentSubtype.UNKNOWN;
    }

    @Override
    public String strategyName() {
        return "generic-extraction";
    }

    @Override
    public List<String> importantFields() {
        return List.of();
    }

    @Override
    public JsonNode expectedSchema() {
        return schema(new LinkedHashMap<>() {{
            put("documentTitle", "string|null");
            put("licensePlate", "string|null");
            put("vin", "string|null");
            put("issueDate", "yyyy-MM-dd|null");
            put("summary", "string|null");
        }});
    }

    @Override
    protected boolean requiresVehicleIdentifier() {
        return false;
    }
}

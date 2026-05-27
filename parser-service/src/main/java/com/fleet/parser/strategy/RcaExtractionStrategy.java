package com.fleet.parser.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleet.parser.dto.DocumentSubtype;
import com.fleet.parser.dto.DocumentType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;

@Component
public class RcaExtractionStrategy extends AbstractDocumentExtractionStrategy {

    public RcaExtractionStrategy(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public DocumentType documentType() {
        return DocumentType.INSURANCE;
    }

    @Override
    public DocumentSubtype subtype() {
        return DocumentSubtype.RCA;
    }

    @Override
    public String strategyName() {
        return "rca-extraction";
    }

    @Override
    public List<String> importantFields() {
        return List.of("policyNumber", "insurerName", "licensePlate", "vin", "validFrom", "validUntil");
    }

    @Override
    public JsonNode expectedSchema() {
        return schema(new LinkedHashMap<>() {{
            put("policyNumber", "string|null");
            put("insurerName", "string|null");
            put("ownerName", "string|null");
            put("licensePlate", "string|null");
            put("vin", "string|null");
            put("validFrom", "yyyy-MM-dd|null");
            put("validUntil", "yyyy-MM-dd|null");
        }});
    }
}

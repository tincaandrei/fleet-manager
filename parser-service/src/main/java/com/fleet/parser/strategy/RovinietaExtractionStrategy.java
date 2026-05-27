package com.fleet.parser.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleet.parser.dto.DocumentSubtype;
import com.fleet.parser.dto.DocumentType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;

@Component
public class RovinietaExtractionStrategy extends AbstractDocumentExtractionStrategy {

    public RovinietaExtractionStrategy(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public DocumentType documentType() {
        return DocumentType.ROAD_TAX;
    }

    @Override
    public DocumentSubtype subtype() {
        return DocumentSubtype.ROVINIETA;
    }

    @Override
    public String strategyName() {
        return "rovinieta-extraction";
    }

    @Override
    public List<String> importantFields() {
        return List.of("licensePlate", "vin", "category", "validFrom", "validUntil", "issuer");
    }

    @Override
    public JsonNode expectedSchema() {
        return schema(new LinkedHashMap<>() {{
            put("licensePlate", "string|null");
            put("vin", "string|null");
            put("category", "string|null");
            put("validFrom", "yyyy-MM-dd|null");
            put("validUntil", "yyyy-MM-dd|null");
            put("issuer", "string|null");
        }});
    }
}

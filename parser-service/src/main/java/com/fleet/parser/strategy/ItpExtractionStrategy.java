package com.fleet.parser.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleet.parser.dto.DocumentSubtype;
import com.fleet.parser.dto.DocumentType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;

@Component
public class ItpExtractionStrategy extends AbstractDocumentExtractionStrategy {

    public ItpExtractionStrategy(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public DocumentType documentType() {
        return DocumentType.TECHNICAL_INSPECTION;
    }

    @Override
    public DocumentSubtype subtype() {
        return DocumentSubtype.ITP;
    }

    @Override
    public String strategyName() {
        return "itp-extraction";
    }

    @Override
    public List<String> importantFields() {
        return List.of("inspectionNumber", "stationName", "licensePlate", "vin", "inspectionDate", "validUntil");
    }

    @Override
    public JsonNode expectedSchema() {
        return schema(new LinkedHashMap<>() {{
            put("inspectionNumber", "string|null");
            put("stationName", "string|null");
            put("licensePlate", "string|null");
            put("vin", "string|null");
            put("inspectionDate", "yyyy-MM-dd|null");
            put("validUntil", "yyyy-MM-dd|null");
        }});
    }
}

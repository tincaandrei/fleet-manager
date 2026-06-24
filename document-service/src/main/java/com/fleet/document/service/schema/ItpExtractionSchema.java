package com.fleet.document.service.schema;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fleet.document.entity.DocumentType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ItpExtractionSchema implements DocumentExtractionSchema {

    private static final List<String> FIELDS = List.of(
            "inspectionNumber", "stationName", "licensePlate", "vin", "inspectionDate",
            "validUntil", "result", "odometerKm", "llmConfidence", "warnings"
    );

    @Override
    public DocumentType documentType() {
        return DocumentType.TECHNICAL_INSPECTION;
    }

    @Override
    public String subtype() {
        return "ITP";
    }

    @Override
    public ObjectNode jsonSchema() {
        Map<String, ObjectNode> properties = new LinkedHashMap<>();
        properties.put("inspectionNumber", JsonSchemaBuilder.nullableString());
        properties.put("stationName", JsonSchemaBuilder.nullableString());
        properties.put("licensePlate", JsonSchemaBuilder.nullableString());
        properties.put("vin", JsonSchemaBuilder.nullableString());
        properties.put("inspectionDate", JsonSchemaBuilder.nullableDate());
        properties.put("validUntil", JsonSchemaBuilder.nullableDate());
        properties.put("result", JsonSchemaBuilder.nullableString());
        properties.put("odometerKm", JsonSchemaBuilder.nullableNumber());
        properties.put("llmConfidence", JsonSchemaBuilder.nullableConfidence());
        properties.put("warnings", JsonSchemaBuilder.stringArray());
        return JsonSchemaBuilder.objectSchema(properties);
    }

    @Override
    public List<String> importantFields() {
        return List.of("inspectionNumber", "stationName", "licensePlate", "vin", "inspectionDate", "validUntil");
    }

    @Override
    public boolean requiresVehicleIdentifier() {
        return true;
    }

    @Override
    public List<String> relevantFields() {
        return FIELDS;
    }

    @Override
    public String promptInstructions() {
        return "Extract Romanian ITP technical inspection data. Normalize inspection dates to yyyy-MM-dd.";
    }
}

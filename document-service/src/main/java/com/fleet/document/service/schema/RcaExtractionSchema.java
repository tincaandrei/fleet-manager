package com.fleet.document.service.schema;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fleet.document.entity.DocumentType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RcaExtractionSchema implements DocumentExtractionSchema {

    private static final List<String> FIELDS = List.of(
            "policyNumber", "insurerName", "ownerName", "licensePlate", "vin",
            "validFrom", "validUntil", "llmConfidence", "warnings"
    );

    @Override
    public DocumentType documentType() {
        return DocumentType.INSURANCE;
    }

    @Override
    public String subtype() {
        return "RCA";
    }

    @Override
    public ObjectNode jsonSchema() {
        Map<String, ObjectNode> properties = new LinkedHashMap<>();
        properties.put("policyNumber", JsonSchemaBuilder.nullableString());
        properties.put("insurerName", JsonSchemaBuilder.nullableString());
        properties.put("ownerName", JsonSchemaBuilder.nullableString());
        properties.put("licensePlate", JsonSchemaBuilder.nullableString());
        properties.put("vin", JsonSchemaBuilder.nullableString());
        properties.put("validFrom", JsonSchemaBuilder.nullableDate());
        properties.put("validUntil", JsonSchemaBuilder.nullableDate());
        properties.put("llmConfidence", JsonSchemaBuilder.nullableConfidence());
        properties.put("warnings", JsonSchemaBuilder.stringArray());
        return JsonSchemaBuilder.objectSchema(properties);
    }

    @Override
    public List<String> importantFields() {
        return List.of("policyNumber", "insurerName", "licensePlate", "vin", "validFrom", "validUntil");
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
        return "Extract RCA insurance policy data from the Romanian vehicle insurance document.";
    }
}

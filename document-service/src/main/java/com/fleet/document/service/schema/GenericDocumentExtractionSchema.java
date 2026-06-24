package com.fleet.document.service.schema;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fleet.document.entity.DocumentType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GenericDocumentExtractionSchema implements DocumentExtractionSchema {

    private static final List<String> FIELDS = List.of(
            "documentTitle", "licensePlate", "vin", "issueDate", "summary", "llmConfidence", "warnings"
    );

    @Override
    public DocumentType documentType() {
        return DocumentType.OTHER;
    }

    @Override
    public String subtype() {
        return "UNKNOWN";
    }

    @Override
    public ObjectNode jsonSchema() {
        Map<String, ObjectNode> properties = new LinkedHashMap<>();
        properties.put("documentTitle", JsonSchemaBuilder.nullableString());
        properties.put("licensePlate", JsonSchemaBuilder.nullableString());
        properties.put("vin", JsonSchemaBuilder.nullableString());
        properties.put("issueDate", JsonSchemaBuilder.nullableDate());
        properties.put("summary", JsonSchemaBuilder.nullableString());
        properties.put("llmConfidence", JsonSchemaBuilder.nullableConfidence());
        properties.put("warnings", JsonSchemaBuilder.stringArray());
        return JsonSchemaBuilder.objectSchema(properties);
    }

    @Override
    public List<String> importantFields() {
        return List.of();
    }

    @Override
    public boolean requiresVehicleIdentifier() {
        return false;
    }

    @Override
    public List<String> relevantFields() {
        return FIELDS;
    }

    @Override
    public String promptInstructions() {
        return "Extract a concise generic summary and any visible vehicle identifiers.";
    }
}

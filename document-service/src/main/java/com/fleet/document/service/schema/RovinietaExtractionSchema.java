package com.fleet.document.service.schema;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fleet.document.entity.DocumentType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RovinietaExtractionSchema implements DocumentExtractionSchema {

    private static final List<String> FIELDS = List.of(
            "licensePlate", "vin", "category", "validFrom", "validUntil", "issuer",
            "transactionId", "amount", "currency", "llmConfidence", "warnings"
    );

    @Override
    public DocumentType documentType() {
        return DocumentType.ROAD_TAX;
    }

    @Override
    public String subtype() {
        return "ROVINIETA";
    }

    @Override
    public ObjectNode jsonSchema() {
        Map<String, ObjectNode> properties = new LinkedHashMap<>();
        properties.put("licensePlate", JsonSchemaBuilder.nullableString());
        properties.put("vin", JsonSchemaBuilder.nullableString());
        properties.put("category", JsonSchemaBuilder.nullableString());
        properties.put("validFrom", JsonSchemaBuilder.nullableDate());
        properties.put("validUntil", JsonSchemaBuilder.nullableDate());
        properties.put("issuer", JsonSchemaBuilder.nullableString());
        properties.put("transactionId", JsonSchemaBuilder.nullableString());
        properties.put("amount", JsonSchemaBuilder.nullableNumber());
        properties.put("currency", JsonSchemaBuilder.nullableStringEnum(List.of("RON", "EUR", "USD")));
        properties.put("llmConfidence", JsonSchemaBuilder.nullableConfidence());
        properties.put("warnings", JsonSchemaBuilder.stringArray());
        return JsonSchemaBuilder.objectSchema(properties);
    }

    @Override
    public List<String> importantFields() {
        return List.of("licensePlate", "vin", "category", "validFrom", "validUntil", "issuer");
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
        return """
                This is a Romanian road tax document.
                Do not treat invoice IDs, transaction IDs, CUI/CIF, serial numbers, order numbers, or CNADNR references as license plates.
                A Romanian plate looks like B12ABC, B123ABC, CJ12ABC, CJ123ABC.
                If no clear vehicle registration plate exists, return licensePlate as null.
                Put payment/order/reference identifiers in transactionId.
                """;
    }
}

package com.fleet.document.service.schema;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fleet.document.entity.DocumentType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class InvoiceExtractionSchema implements DocumentExtractionSchema {

    private static final List<String> FIELDS = List.of(
            "invoiceNumber", "supplierName", "supplierTaxId", "invoiceDate", "totalAmount",
            "currency", "vatAmount", "expenseCategory", "licensePlate", "vin", "odometerKm",
            "items", "llmConfidence", "warnings"
    );

    @Override
    public DocumentType documentType() {
        return DocumentType.EXPENSE_INVOICE;
    }

    @Override
    public String subtype() {
        return "UNKNOWN";
    }

    @Override
    public ObjectNode jsonSchema() {
        Map<String, ObjectNode> properties = new LinkedHashMap<>();
        properties.put("invoiceNumber", JsonSchemaBuilder.nullableString());
        properties.put("supplierName", JsonSchemaBuilder.nullableString());
        properties.put("supplierTaxId", JsonSchemaBuilder.nullableString());
        properties.put("invoiceDate", JsonSchemaBuilder.nullableDate());
        properties.put("totalAmount", JsonSchemaBuilder.nullableNumber());
        properties.put("currency", JsonSchemaBuilder.nullableStringEnum(List.of("RON", "EUR", "USD")));
        properties.put("vatAmount", JsonSchemaBuilder.nullableNumber());
        properties.put("expenseCategory", JsonSchemaBuilder.nullableStringEnum(List.of(
                "FUEL", "SERVICE", "TIRE_REPLACEMENT", "PARTS", "CAR_WASH",
                "PARKING", "ROAD_TAX", "INSURANCE", "OTHER_EXPENSE"
        )));
        properties.put("licensePlate", JsonSchemaBuilder.nullableString());
        properties.put("vin", JsonSchemaBuilder.nullableString());
        properties.put("odometerKm", JsonSchemaBuilder.nullableNumber());
        properties.put("items", JsonSchemaBuilder.invoiceItemsArray());
        properties.put("llmConfidence", JsonSchemaBuilder.nullableConfidence());
        properties.put("warnings", JsonSchemaBuilder.stringArray());
        return JsonSchemaBuilder.objectSchema(properties);
    }

    @Override
    public List<String> importantFields() {
        return List.of("invoiceNumber", "supplierName", "invoiceDate", "totalAmount", "currency", "expenseCategory");
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
        return "Extract expense invoice data. Classify expenseCategory only when clearly supported by the invoice text.";
    }
}

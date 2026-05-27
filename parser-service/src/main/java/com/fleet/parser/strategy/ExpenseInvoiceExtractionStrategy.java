package com.fleet.parser.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleet.parser.dto.DocumentSubtype;
import com.fleet.parser.dto.DocumentType;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;

@Component
public class ExpenseInvoiceExtractionStrategy extends AbstractDocumentExtractionStrategy {

    public ExpenseInvoiceExtractionStrategy(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public DocumentType documentType() {
        return DocumentType.EXPENSE_INVOICE;
    }

    @Override
    public DocumentSubtype subtype() {
        return DocumentSubtype.UNKNOWN;
    }

    @Override
    public String strategyName() {
        return "expense-invoice-extraction";
    }

    @Override
    public List<String> importantFields() {
        return List.of("invoiceNumber", "supplierName", "invoiceDate", "totalAmount", "currency", "expenseCategory");
    }

    @Override
    public JsonNode expectedSchema() {
        return schema(new LinkedHashMap<>() {{
            put("invoiceNumber", "string|null");
            put("supplierName", "string|null");
            put("supplierTaxId", "string|null");
            put("invoiceDate", "yyyy-MM-dd|null");
            put("totalAmount", "number|null");
            put("currency", "RON|EUR|USD|null");
            put("vatAmount", "number|null");
            put("expenseCategory", "FUEL|SERVICE|TIRE_REPLACEMENT|PARTS|CAR_WASH|PARKING|OTHER_EXPENSE|null");
            put("licensePlate", "string|null");
            put("vin", "string|null");
            put("odometerKm", "number|null");
            put("items", "array of {description, quantity, unit, unitPrice, totalPrice}");
        }});
    }

    @Override
    protected boolean requiresVehicleIdentifier() {
        return false;
    }
}

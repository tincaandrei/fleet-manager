package com.fleet.document.service.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fleet.document.entity.DocumentType;
import com.fleet.document.service.parsing.DocumentTypeDetection;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentExtractionSchemaRegistryTest {

    private final RcaExtractionSchema rca = new RcaExtractionSchema();
    private final ItpExtractionSchema itp = new ItpExtractionSchema();
    private final RovinietaExtractionSchema rovinieta = new RovinietaExtractionSchema();
    private final InvoiceExtractionSchema invoice = new InvoiceExtractionSchema();
    private final GenericDocumentExtractionSchema generic = new GenericDocumentExtractionSchema();
    private final DocumentExtractionSchemaRegistry registry = new DocumentExtractionSchemaRegistry(
            rca,
            itp,
            rovinieta,
            invoice,
            generic
    );

    @Test
    void returnsCorrectSchemaForEachDetection() {
        assertThat(registry.schemaFor(new DocumentTypeDetection(DocumentType.ROAD_TAX, "ROVINIETA", 0.9, List.of()))).isSameAs(rovinieta);
        assertThat(registry.schemaFor(new DocumentTypeDetection(DocumentType.INSURANCE, "RCA", 0.9, List.of()))).isSameAs(rca);
        assertThat(registry.schemaFor(new DocumentTypeDetection(DocumentType.TECHNICAL_INSPECTION, "ITP", 0.9, List.of()))).isSameAs(itp);
        assertThat(registry.schemaFor(new DocumentTypeDetection(DocumentType.EXPENSE_INVOICE, "UNKNOWN", 0.9, List.of()))).isSameAs(invoice);
        assertThat(registry.schemaFor(new DocumentTypeDetection(DocumentType.OTHER, "UNKNOWN", 0.3, List.of()))).isSameAs(generic);
    }

    @Test
    void schemasAreStrictAndDoNotContainPseudoDescriptors() {
        for (DocumentExtractionSchema schema : List.of(rca, itp, rovinieta, invoice, generic)) {
            JsonNode jsonSchema = schema.jsonSchema();

            assertThat(jsonSchema.path("additionalProperties").asBoolean()).isFalse();
            assertThat(jsonSchema.path("required").size()).isGreaterThan(0);
            assertThat(containsText(jsonSchema, "string|null")).isFalse();
            assertThat(containsText(jsonSchema, "number|null")).isFalse();
            assertThat(containsText(jsonSchema, "yyyy-MM-dd|null")).isFalse();
        }
    }

    private boolean containsText(JsonNode node, String text) {
        if (node == null || node.isNull()) {
            return false;
        }
        if (node.isTextual()) {
            return text.equals(node.asText());
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                if (containsText(fields.next().getValue(), text)) {
                    return true;
                }
            }
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                if (containsText(child, text)) {
                    return true;
                }
            }
        }
        return false;
    }
}

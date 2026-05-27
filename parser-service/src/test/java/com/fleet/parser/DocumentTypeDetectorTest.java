package com.fleet.parser;

import com.fleet.parser.dto.DocumentSubtype;
import com.fleet.parser.dto.DocumentType;
import com.fleet.parser.service.DocumentTypeDetector;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentTypeDetectorTest {

    private final DocumentTypeDetector detector = new DocumentTypeDetector();

    @Test
    void detectsExpenseInvoiceFromRomanianInvoiceText() {
        var result = detector.detect(null, "Factura fiscala CUI RO123 TVA total bon fiscal carburant");

        assertThat(result.documentType()).isEqualTo(DocumentType.EXPENSE_INVOICE);
        assertThat(result.subtype()).isEqualTo(DocumentSubtype.UNKNOWN);
    }

    @Test
    void declaredTypeWinsWhenValid() {
        var result = detector.detect("ITP", "Factura fiscala");

        assertThat(result.documentType()).isEqualTo(DocumentType.TECHNICAL_INSPECTION);
        assertThat(result.subtype()).isEqualTo(DocumentSubtype.ITP);
    }
}

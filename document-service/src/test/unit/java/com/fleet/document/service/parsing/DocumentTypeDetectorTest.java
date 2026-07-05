package com.fleet.document.service.parsing;

import com.fleet.document.entity.DocumentType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentTypeDetectorTest {

    private final DocumentTypeDetector detector = new DocumentTypeDetector();

    @Test
    void detectsRovinietaAsRoadTax() {
        DocumentTypeDetection detection = detector.detect("Rovinieta CNAIR tarif de utilizare pentru B123ABC");

        assertThat(detection.documentType()).isEqualTo(DocumentType.ROAD_TAX);
        assertThat(detection.subtype()).isEqualTo("ROVINIETA");
        assertThat(detection.matchedKeywords()).isNotEmpty();
    }

    @Test
    void detectsRcaAsInsurance() {
        DocumentTypeDetection detection = detector.detect("Polita RCA asigurator carte verde raspundere civila auto");

        assertThat(detection.documentType()).isEqualTo(DocumentType.INSURANCE);
        assertThat(detection.subtype()).isEqualTo("RCA");
    }

    @Test
    void detectsItpAsTechnicalInspection() {
        DocumentTypeDetection detection = detector.detect("Certificat ITP inspectie tehnica periodica RAR");

        assertThat(detection.documentType()).isEqualTo(DocumentType.TECHNICAL_INSPECTION);
        assertThat(detection.subtype()).isEqualTo("ITP");
    }

    @Test
    void detectsInvoiceAsExpenseInvoice() {
        DocumentTypeDetection detection = detector.detect("Factura fiscala TVA total de plata");

        assertThat(detection.documentType()).isEqualTo(DocumentType.EXPENSE_INVOICE);
    }

    @Test
    void rovinietaWinsOverInvoiceWords() {
        DocumentTypeDetection detection = detector.detect("Factura pentru rovinieta CNADNR TVA total");

        assertThat(detection.documentType()).isEqualTo(DocumentType.ROAD_TAX);
        assertThat(detection.subtype()).isEqualTo("ROVINIETA");
    }
}

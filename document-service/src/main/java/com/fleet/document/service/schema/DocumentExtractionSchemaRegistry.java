package com.fleet.document.service.schema;

import com.fleet.document.entity.DocumentType;
import com.fleet.document.service.parsing.DocumentTypeDetection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocumentExtractionSchemaRegistry {

    private final RcaExtractionSchema rcaExtractionSchema;
    private final ItpExtractionSchema itpExtractionSchema;
    private final RovinietaExtractionSchema rovinietaExtractionSchema;
    private final InvoiceExtractionSchema invoiceExtractionSchema;
    private final GenericDocumentExtractionSchema genericDocumentExtractionSchema;

    public DocumentExtractionSchema schemaFor(DocumentTypeDetection detection) {
        if (detection.documentType() == DocumentType.ROAD_TAX && "ROVINIETA".equalsIgnoreCase(detection.subtype())) {
            return rovinietaExtractionSchema;
        }
        if (detection.documentType() == DocumentType.INSURANCE && "RCA".equalsIgnoreCase(detection.subtype())) {
            return rcaExtractionSchema;
        }
        if (detection.documentType() == DocumentType.TECHNICAL_INSPECTION && "ITP".equalsIgnoreCase(detection.subtype())) {
            return itpExtractionSchema;
        }
        if (detection.documentType() == DocumentType.EXPENSE_INVOICE) {
            return invoiceExtractionSchema;
        }
        return genericDocumentExtractionSchema;
    }
}

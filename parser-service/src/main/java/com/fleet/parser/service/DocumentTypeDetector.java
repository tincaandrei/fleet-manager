package com.fleet.parser.service;

import com.fleet.parser.dto.DocumentSubtype;
import com.fleet.parser.dto.DocumentType;
import com.fleet.parser.dto.DocumentTypeDetectionResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.Locale;

@Service
public class DocumentTypeDetector {

    public DocumentTypeDetectionResult detect(String declaredDocumentType, String text) {
        DocumentTypeDetectionResult declared = fromDeclaredType(declaredDocumentType);
        if (declared != null && declared.documentType() != DocumentType.OTHER) {
            return declared;
        }

        String normalized = normalize(text);
        if (containsAny(normalized, "factura", "bon fiscal", "invoice", "receipt", "tva", "cui")) {
            return new DocumentTypeDetectionResult(DocumentType.EXPENSE_INVOICE, DocumentSubtype.UNKNOWN);
        }
        if (containsAny(normalized, "rovigneta", "rovinieta", "tarif de utilizare", "categoria vehiculului")) {
            return new DocumentTypeDetectionResult(DocumentType.ROAD_TAX, DocumentSubtype.ROVINIETA);
        }
        if (containsAny(normalized, "inspectie tehnica periodica", "itp", "rar", "valabilitate inspectie")) {
            return new DocumentTypeDetectionResult(DocumentType.TECHNICAL_INSPECTION, DocumentSubtype.ITP);
        }
        if (containsAny(normalized, "asigurare", "polita", "rca", "carte verde", "bonus malus")) {
            return new DocumentTypeDetectionResult(DocumentType.INSURANCE, DocumentSubtype.RCA);
        }
        return new DocumentTypeDetectionResult(DocumentType.OTHER, DocumentSubtype.UNKNOWN);
    }

    private DocumentTypeDetectionResult fromDeclaredType(String declaredDocumentType) {
        if (!StringUtils.hasText(declaredDocumentType)) {
            return null;
        }
        String normalized = normalize(declaredDocumentType)
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "RCA", "INSURANCE" -> new DocumentTypeDetectionResult(DocumentType.INSURANCE, DocumentSubtype.RCA);
            case "ITP", "INSPECTION", "TECHNICAL_INSPECTION" ->
                    new DocumentTypeDetectionResult(DocumentType.TECHNICAL_INSPECTION, DocumentSubtype.ITP);
            case "ROVINIETA", "ROVIGNETA", "ROAD_TAX" ->
                    new DocumentTypeDetectionResult(DocumentType.ROAD_TAX, DocumentSubtype.ROVINIETA);
            case "EXPENSE_INVOICE", "INVOICE", "FACTURA", "BON_FISCAL", "RECEIPT" ->
                    new DocumentTypeDetectionResult(DocumentType.EXPENSE_INVOICE, DocumentSubtype.UNKNOWN);
            case "OTHER", "UNKNOWN" -> new DocumentTypeDetectionResult(DocumentType.OTHER, DocumentSubtype.UNKNOWN);
            default -> null;
        };
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String noDiacritics = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return noDiacritics.toLowerCase(Locale.ROOT);
    }
}

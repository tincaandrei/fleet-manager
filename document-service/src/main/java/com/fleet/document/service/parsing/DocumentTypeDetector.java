package com.fleet.document.service.parsing;

import com.fleet.document.entity.DocumentType;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class DocumentTypeDetector {

    public DocumentTypeDetection detect(String text) {
        String normalized = normalizeForDetection(text);

        List<String> roadTaxMatches = matches(normalized,
                "rovigneta", "rovinieta", "roviniete", "erovinieta", "tarif de utilizare", "cnair", "cnadnr");
        if (!roadTaxMatches.isEmpty()) {
            return detection(DocumentType.ROAD_TAX, "ROVINIETA", roadTaxMatches);
        }

        List<String> insuranceMatches = matches(normalized,
                "asigurare", "asigurari", "asigurator", "polita", "carte verde", "raspundere civila auto");
        if (containsToken(normalized, "rca")) {
            insuranceMatches.add("rca");
        }
        if (!insuranceMatches.isEmpty()) {
            return detection(DocumentType.INSURANCE, "RCA", insuranceMatches);
        }

        List<String> inspectionMatches = matches(normalized, "inspectie tehnica periodica", "valabilitate inspectie");
        if (containsToken(normalized, "itp")) {
            inspectionMatches.add("itp");
        }
        if (containsToken(normalized, "rar")) {
            inspectionMatches.add("rar");
        }
        if (!inspectionMatches.isEmpty()) {
            return detection(DocumentType.TECHNICAL_INSPECTION, "ITP", inspectionMatches);
        }

        List<String> invoiceMatches = matches(normalized, "factura", "bon fiscal", "invoice", "receipt", "tva");
        if (!invoiceMatches.isEmpty()) {
            return detection(DocumentType.EXPENSE_INVOICE, "UNKNOWN", invoiceMatches);
        }

        return new DocumentTypeDetection(DocumentType.OTHER, "UNKNOWN", 0.3, List.of());
    }

    private DocumentTypeDetection detection(DocumentType documentType, String subtype, List<String> matchedKeywords) {
        double confidence = Math.min(0.95, 0.65 + (matchedKeywords.size() * 0.1));
        return new DocumentTypeDetection(documentType, subtype, confidence, List.copyOf(matchedKeywords));
    }

    private List<String> matches(String value, String... keywords) {
        List<String> matched = new ArrayList<>();
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                matched.add(keyword);
            }
        }
        return matched;
    }

    private boolean containsToken(String value, String token) {
        return Pattern.compile("(?<![a-z0-9])" + Pattern.quote(token) + "(?![a-z0-9])").matcher(value).find();
    }

    private String normalizeForDetection(String value) {
        if (value == null) {
            return "";
        }
        String noDiacritics = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noDiacritics.toLowerCase(Locale.ROOT);
    }
}

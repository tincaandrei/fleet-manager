package com.fleet.document.service;

import com.fleet.document.entity.TextExtractionMethod;
import com.fleet.document.entity.VehicleDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentTextExtractionService {

    private static final Pattern DOCUMENT_KEYWORDS = Pattern.compile(
            "\\b(rovigneta|rovinieta|roviniete|erovinieta|cnadnr|rca|asigurare|asigurator|polita|carte verde|factura|invoice|bon fiscal|tva|inspectie|tehnica|periodica|itp|rar)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private final DocumentStorageService storageService;
    private final PdfBoxTextExtractionService pdfBoxTextExtractionService;
    private final OcrTextExtractionService ocrTextExtractionService;

    @Value("${document.parser.minimum-text-length}")
    private int minimumTextLength;

    @Value("${document.parser.good-text-length}")
    private int goodTextLength;

    public ExtractedTextResult extractText(VehicleDocument document) {
        byte[] content = loadContent(document);

        try {
            ExtractedTextResult pdfBoxResult = withQuality(pdfBoxTextExtractionService.extract(content));
            log.info(
                    "PDFBox extraction completed for document {} with text length {} and quality {}",
                    document.getId(),
                    length(pdfBoxResult.text()),
                    pdfBoxResult.textQualityScore()
            );
            if (isUsable(pdfBoxResult.text())) {
                log.info("Document {} extracted with PDFBox", document.getId());
                return pdfBoxResult;
            }
        } catch (TextExtractionException exception) {
            log.warn("PDFBox extraction failed for document {} with code {}", document.getId(), exception.errorCode());
        }

        log.info("Starting OCR fallback for document {}", document.getId());
        try {
            ExtractedTextResult ocrResult = withQuality(ocrTextExtractionService.extract(content));
            List<String> warnings = new ArrayList<>(ocrResult.warnings() == null ? List.of() : ocrResult.warnings());
            warnings.add("OCR extraction may contain errors. Please verify manually.");
            if (!isUsable(ocrResult.text())) {
                throw new TextExtractionException("OCR_TEXT_UNREADABLE", "OCR did not produce readable text");
            }
            log.info(
                    "OCR extraction completed for document {} with text length {} and quality {}",
                    document.getId(),
                    length(ocrResult.text()),
                    ocrResult.textQualityScore()
            );
            return new ExtractedTextResult(
                    ocrResult.text(),
                    TextExtractionMethod.OCR,
                    ocrResult.pageCount(),
                    warnings,
                    ocrResult.textQualityScore()
            );
        } catch (TextExtractionException ocrFailure) {
            log.warn("OCR extraction failed for document {} with code {}", document.getId(), ocrFailure.errorCode());
            throw ocrFailure;
        }
    }

    boolean isUsable(String text) {
        if (text == null || text.isBlank() || text.length() < minimumTextLength) {
            return false;
        }
        long alphabeticCharacters = text.chars().filter(Character::isLetter).count();
        if (alphabeticCharacters < Math.min(20, minimumTextLength / 2)) {
            return false;
        }
        double usefulRatio = usefulCharacterRatio(text);
        if (usefulRatio < 0.65 || replacementRatio(text) > 0.02) {
            return false;
        }
        return text.length() >= goodTextLength || DOCUMENT_KEYWORDS.matcher(normalizeForDetection(text)).find();
    }

    private byte[] loadContent(VehicleDocument document) {
        Resource resource = storageService.load(document.getStoragePath());
        try {
            return resource.getInputStream().readAllBytes();
        } catch (IOException exception) {
            throw new TextExtractionException("FILE_READ_FAILED", "Could not load stored document file", exception);
        }
    }

    private ExtractedTextResult withQuality(ExtractedTextResult result) {
        return new ExtractedTextResult(
                result.text(),
                result.extractionMethod(),
                result.pageCount(),
                result.warnings(),
                calculateTextQualityScore(result.text())
        );
    }

    private double calculateTextQualityScore(String text) {
        if (text == null || text.isBlank()) {
            return 0.0;
        }
        int lengthScoreBase = Math.min(text.length(), goodTextLength);
        double lengthScore = (double) lengthScoreBase / goodTextLength;
        double replacementPenalty = text.contains("\uFFFD") ? 0.15 : 0.0;
        return clamp((lengthScore * 0.65) + (usefulCharacterRatio(text) * 0.35) - replacementPenalty);
    }

    private double usefulCharacterRatio(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0;
        }
        long usefulCharacters = text.chars()
                .filter(character -> Character.isLetterOrDigit(character) || Character.isWhitespace(character))
                .count();
        return (double) usefulCharacters / text.length();
    }

    private double replacementRatio(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0;
        }
        long replacements = text.chars().filter(character -> character == '\uFFFD').count();
        return (double) replacements / text.length();
    }

    private String normalizeForDetection(String value) {
        if (value == null) {
            return "";
        }
        String noDiacritics = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noDiacritics.toLowerCase(Locale.ROOT);
    }

    private int length(String text) {
        return text == null ? 0 : text.length();
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}

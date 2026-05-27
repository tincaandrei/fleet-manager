package com.fleet.parser.service;

import com.fleet.parser.config.ParserProperties;
import com.fleet.parser.dto.ExtractionMethod;
import com.fleet.parser.dto.TextExtractionResult;
import com.fleet.parser.exception.TextExtractionException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class PdfTextExtractorService {

    private final ParserProperties parserProperties;

    public PdfTextExtractorService(ParserProperties parserProperties) {
        this.parserProperties = parserProperties;
    }

    public TextExtractionResult extractText(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = normalizeWhitespace(stripper.getText(document));
            return new TextExtractionResult(text, ExtractionMethod.PDF_TEXT, calculateTextQualityScore(text));
        } catch (IOException exception) {
            throw new TextExtractionException("Could not extract readable text from PDF", exception);
        }
    }

    public boolean isReadableEnough(String text) {
        return text != null && text.length() >= parserProperties.getMinimumTextLength();
    }

    public double calculateTextQualityScore(String text) {
        if (text == null || text.isBlank()) {
            return 0.0;
        }

        int lengthScoreBase = Math.min(text.length(), parserProperties.getGoodTextLength());
        double lengthScore = (double) lengthScoreBase / parserProperties.getGoodTextLength();
        long usefulCharacters = text.chars()
                .filter(character -> Character.isLetterOrDigit(character) || Character.isWhitespace(character))
                .count();
        double usefulRatio = (double) usefulCharacters / text.length();
        double replacementPenalty = text.contains("\uFFFD") ? 0.15 : 0.0;

        return clamp((lengthScore * 0.65) + (usefulRatio * 0.35) - replacementPenalty);
    }

    private String normalizeWhitespace(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\u00A0', ' ')
                .replaceAll("[\\t\\x0B\\f\\r]+", " ")
                .replaceAll(" +", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}

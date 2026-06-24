package com.fleet.document.service.parsing;

import com.fleet.document.entity.TextExtractionMethod;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
public class PdfBoxTextExtractionService {

    public ExtractedTextResult extract(byte[] content) {
        try (PDDocument pdf = Loader.loadPDF(content)) {
            if (pdf.isEncrypted()) {
                throw new TextExtractionException("ENCRYPTED_PDF", "PDF is encrypted and cannot be parsed automatically");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            String text = normalizeWhitespace(stripper.getText(pdf));
            return new ExtractedTextResult(text, TextExtractionMethod.PDFBOX, pdf.getNumberOfPages(), List.of(), 0.0);
        } catch (TextExtractionException exception) {
            throw exception;
        } catch (IOException exception) {
            log.warn("PDFBox text extraction failed", exception);
            throw new TextExtractionException("TEXT_EXTRACTION_FAILED", "Could not extract readable text from PDF", exception);
        }
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
}

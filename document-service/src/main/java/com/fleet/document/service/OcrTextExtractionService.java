package com.fleet.document.service;

import com.fleet.document.entity.TextExtractionMethod;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class OcrTextExtractionService {

    private final String language;
    private final int dpi;
    private final String tessdataPath;

    public OcrTextExtractionService(
            @Value("${document.ocr.language}") String language,
            @Value("${document.ocr.dpi}") int dpi,
            @Value("${document.ocr.tessdata-path}") String tessdataPath
    ) {
        this.language = language;
        this.dpi = dpi;
        this.tessdataPath = tessdataPath;
    }

    public ExtractedTextResult extract(byte[] content) {
        try (PDDocument pdf = Loader.loadPDF(content)) {
            if (pdf.isEncrypted()) {
                throw new TextExtractionException("ENCRYPTED_PDF", "PDF is encrypted and cannot be parsed automatically");
            }
            PDFRenderer renderer = new PDFRenderer(pdf);
            ITesseract tesseract = tesseract();
            List<String> pageTexts = new ArrayList<>();
            for (int pageIndex = 0; pageIndex < pdf.getNumberOfPages(); pageIndex++) {
                BufferedImage image = renderer.renderImageWithDPI(pageIndex, dpi, ImageType.RGB);
                pageTexts.add(normalizeWhitespace(tesseract.doOCR(image)));
            }
            return new ExtractedTextResult(
                    normalizeWhitespace(String.join("\n\n", pageTexts)),
                    TextExtractionMethod.OCR,
                    pdf.getNumberOfPages(),
                    List.of(),
                    0.0
            );
        } catch (TextExtractionException exception) {
            throw exception;
        } catch (TesseractException exception) {
            log.warn("Tesseract OCR failed", exception);
            throw new TextExtractionException("OCR_FAILED", "OCR failed while reading the PDF", exception);
        } catch (IOException exception) {
            log.warn("PDF rendering for OCR failed", exception);
            throw new TextExtractionException("OCR_FAILED", "Could not render PDF pages for OCR", exception);
        } catch (RuntimeException exception) {
            log.warn("Unexpected OCR failure", exception);
            throw new TextExtractionException("OCR_FAILED", "OCR failed while reading the PDF", exception);
        }
    }

    private ITesseract tesseract() {
        Tesseract tesseract = new Tesseract();
        tesseract.setLanguage(language);
        if (StringUtils.hasText(tessdataPath)) {
            tesseract.setDatapath(tessdataPath);
        }
        return tesseract;
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

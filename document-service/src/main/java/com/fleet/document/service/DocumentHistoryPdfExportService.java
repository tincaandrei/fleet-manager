package com.fleet.document.service;

import com.fleet.document.dto.DocumentHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentHistoryPdfExportService {

    private static final float MARGIN = 34;
    private static final float ROW_HEIGHT = 22;
    private static final float FONT_SIZE = 8;
    private static final float HEADER_FONT_SIZE = 10;
    private static final PDType1Font FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    public byte[] export(String title, List<DocumentHistoryResponse> items, boolean includeBusiness) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfPageState state = newPage(document, title, items.size(), includeBusiness);
            for (DocumentHistoryResponse item : items) {
                if (state.y < MARGIN + ROW_HEIGHT) {
                    state.contentStream.close();
                    state = newPage(document, title, items.size(), includeBusiness);
                }
                writeRow(state, item, includeBusiness);
            }
            state.contentStream.close();
            document.save(out);
            return out.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not export document history PDF", exception);
        }
    }

    private PdfPageState newPage(PDDocument document, String title, int totalItems, boolean includeBusiness) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        page.setRotation(90);
        document.addPage(page);
        PDRectangle pageSize = page.getMediaBox();
        float width = pageSize.getHeight();
        float height = pageSize.getWidth();
        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        contentStream.transform(new org.apache.pdfbox.util.Matrix(0, 1, -1, 0, pageSize.getWidth(), 0));

        PdfPageState state = new PdfPageState(contentStream, width, height, height - MARGIN);
        writeText(state, title, MARGIN, state.y, 16, FONT_BOLD);
        state.y -= 20;
        writeText(state, "Generated: " + DATE_TIME_FORMATTER.format(java.time.Instant.now()), MARGIN, state.y, 9, FONT);
        writeText(state, "Total documents: " + totalItems, MARGIN + 220, state.y, 9, FONT);
        state.y -= 28;
        writeTableHeader(state, includeBusiness);
        return state;
    }

    private void writeTableHeader(PdfPageState state, boolean includeBusiness) throws IOException {
        String[] headers = includeBusiness
                ? new String[]{"Uploaded", "Uploader", "File", "Type", "Status", "Vehicle", "Org"}
                : new String[]{"Uploaded", "Uploader", "File", "Type", "Status", "Vehicle"};
        float[] widths = columnWidths(includeBusiness);
        float x = MARGIN;
        for (int i = 0; i < headers.length; i++) {
            writeText(state, headers[i], x, state.y, HEADER_FONT_SIZE, FONT_BOLD);
            x += widths[i];
        }
        state.y -= 14;
    }

    private void writeRow(PdfPageState state, DocumentHistoryResponse item, boolean includeBusiness) throws IOException {
        String[] values = includeBusiness
                ? new String[]{
                        formatDate(item),
                        uploaderLabel(item),
                        item.originalFileName(),
                        documentTypeLabel(item),
                        item.status() == null ? "-" : item.status().name(),
                        "Vehicle #" + item.vehicleId(),
                        item.businessId() == null ? "-" : item.businessId().toString()
                }
                : new String[]{
                        formatDate(item),
                        uploaderLabel(item),
                        item.originalFileName(),
                        documentTypeLabel(item),
                        item.status() == null ? "-" : item.status().name(),
                        "Vehicle #" + item.vehicleId()
                };
        float[] widths = columnWidths(includeBusiness);
        float x = MARGIN;
        for (int i = 0; i < values.length; i++) {
            writeWrappedText(state, values[i], x, state.y, widths[i] - 8);
            x += widths[i];
        }
        state.y -= ROW_HEIGHT;
    }

    private float[] columnWidths(boolean includeBusiness) {
        return includeBusiness
                ? new float[]{82, 142, 178, 100, 88, 76, 42}
                : new float[]{88, 160, 220, 110, 94, 76};
    }

    private String formatDate(DocumentHistoryResponse item) {
        return item.uploadedAt() == null ? "-" : DATE_TIME_FORMATTER.format(item.uploadedAt());
    }

    private String uploaderLabel(DocumentHistoryResponse item) {
        if (item.uploadedByUsername() != null && item.uploadedByEmail() != null) {
            return item.uploadedByUsername() + " (" + item.uploadedByEmail() + ")";
        }
        if (item.uploadedByUsername() != null) {
            return item.uploadedByUsername();
        }
        if (item.uploadedByEmail() != null) {
            return item.uploadedByEmail();
        }
        return item.uploadedByUserId() == null ? "Unknown user" : "User #" + item.uploadedByUserId();
    }

    private String documentTypeLabel(DocumentHistoryResponse item) {
        String type = item.documentType() == null ? "-" : item.documentType().name();
        return item.documentSubtype() == null || item.documentSubtype().isBlank()
                ? type
                : type + " / " + item.documentSubtype();
    }

    private void writeWrappedText(PdfPageState state, String value, float x, float y, float width) throws IOException {
        List<String> lines = wrap(value == null ? "-" : value, width);
        for (int i = 0; i < Math.min(lines.size(), 2); i++) {
            writeText(state, lines.get(i), x, y - (i * 9), FONT_SIZE, FONT);
        }
    }

    private List<String> wrap(String value, float width) throws IOException {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : value.split("\\s+")) {
            String next = line.length() == 0 ? word : line + " " + word;
            if (textWidth(next, FONT_SIZE) <= width) {
                line = new StringBuilder(next);
            } else {
                if (line.length() > 0) {
                    lines.add(line.toString());
                }
                line = new StringBuilder(trimToWidth(word, width));
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines.isEmpty() ? List.of("-") : lines;
    }

    private String trimToWidth(String value, float width) throws IOException {
        String text = value;
        while (text.length() > 1 && textWidth(text + "...", FONT_SIZE) > width) {
            text = text.substring(0, text.length() - 1);
        }
        return text.equals(value) ? text : text + "...";
    }

    private float textWidth(String text, float fontSize) throws IOException {
        return FONT.getStringWidth(sanitize(text)) / 1000 * fontSize;
    }

    private void writeText(PdfPageState state, String text, float x, float y, float fontSize, PDType1Font font) throws IOException {
        state.contentStream.beginText();
        state.contentStream.setFont(font, fontSize);
        state.contentStream.newLineAtOffset(x, y);
        state.contentStream.showText(sanitize(text));
        state.contentStream.endText();
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replaceAll("[^\\x20-\\x7E]", "?");
    }

    private static class PdfPageState {
        private final PDPageContentStream contentStream;
        private final float width;
        private final float height;
        private float y;

        private PdfPageState(PDPageContentStream contentStream, float width, float height, float y) {
            this.contentStream = contentStream;
            this.width = width;
            this.height = height;
            this.y = y;
        }
    }
}

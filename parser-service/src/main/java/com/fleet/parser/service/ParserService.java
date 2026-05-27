package com.fleet.parser.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fleet.parser.dto.DocumentTypeDetectionResult;
import com.fleet.parser.dto.ExtractionMethod;
import com.fleet.parser.dto.NormalizedExtractionResult;
import com.fleet.parser.dto.ParserErrorCode;
import com.fleet.parser.dto.ParserExtractionCommand;
import com.fleet.parser.dto.ParserExtractionResponse;
import com.fleet.parser.dto.TextExtractionResult;
import com.fleet.parser.exception.ParserException;
import com.fleet.parser.strategy.DocumentExtractionStrategy;
import com.fleet.parser.strategy.GenericExtractionStrategy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ParserService {

    private final PdfTextExtractorService pdfTextExtractorService;
    private final OcrService ocrService;
    private final DocumentTypeDetector documentTypeDetector;
    private final PromptBuilder promptBuilder;
    private final OllamaClient ollamaClient;
    private final ConfidenceCalculator confidenceCalculator;
    private final ParserResponseMapper responseMapper;
    private final List<DocumentExtractionStrategy> strategies;
    private final GenericExtractionStrategy genericExtractionStrategy;

    public ParserService(PdfTextExtractorService pdfTextExtractorService,
                         OcrService ocrService,
                         DocumentTypeDetector documentTypeDetector,
                         PromptBuilder promptBuilder,
                         OllamaClient ollamaClient,
                         ConfidenceCalculator confidenceCalculator,
                         ParserResponseMapper responseMapper,
                         List<DocumentExtractionStrategy> strategies,
                         GenericExtractionStrategy genericExtractionStrategy) {
        this.pdfTextExtractorService = pdfTextExtractorService;
        this.ocrService = ocrService;
        this.documentTypeDetector = documentTypeDetector;
        this.promptBuilder = promptBuilder;
        this.ollamaClient = ollamaClient;
        this.confidenceCalculator = confidenceCalculator;
        this.responseMapper = responseMapper;
        this.strategies = strategies;
        this.genericExtractionStrategy = genericExtractionStrategy;
    }

    public ParserExtractionResponse extract(ParserExtractionCommand command) {
        try {
            validateRequest(command);

            TextExtractionResult textExtraction = extractReadableText(command.file());
            DocumentTypeDetectionResult detection = documentTypeDetector.detect(
                    command.declaredDocumentType(),
                    textExtraction.text()
            );
            DocumentExtractionStrategy strategy = selectStrategy(detection);
            String prompt = promptBuilder.buildPrompt(strategy, textExtraction.text());
            JsonNode rawModelData = unwrapExtractedData(ollamaClient.extractJson(prompt));
            NormalizedExtractionResult normalized = normalizeAndValidate(strategy, rawModelData);

            double confidence = confidenceCalculator.calculate(
                    strategy,
                    normalized.extractedData(),
                    textExtraction.extractionMethod(),
                    textExtraction.textQualityScore(),
                    normalized.llmConfidence(),
                    normalized.warnings()
            );

            return responseMapper.success(
                    command.documentId(),
                    detection.documentType(),
                    detection.subtype(),
                    confidence,
                    textExtraction.extractionMethod(),
                    normalized.extractedData(),
                    normalized.warnings()
            );
        } catch (ParserException exception) {
            return responseMapper.failure(command.documentId(), exception.getErrorCode(), exception.getMessage(), List.of(exception.getMessage()));
        } catch (Exception exception) {
            return responseMapper.failure(command.documentId(), ParserErrorCode.INTERNAL_ERROR, "Unexpected parser-service error", List.of("Unexpected parser-service error"));
        }
    }

    private TextExtractionResult extractReadableText(MultipartFile file) {
        TextExtractionResult pdfText = pdfTextExtractorService.extractText(file);
        if (pdfTextExtractorService.isReadableEnough(pdfText.text())) {
            return pdfText;
        }

        if (!ocrService.isEnabled()) {
            throw new ParserException(ParserErrorCode.TEXT_EXTRACTION_FAILED, "Could not extract readable text from PDF");
        }

        String ocrText = ocrService.extractText(file);
        if (!StringUtils.hasText(ocrText) || !pdfTextExtractorService.isReadableEnough(ocrText)) {
            throw new ParserException(ParserErrorCode.OCR_FAILED, "OCR did not produce readable text");
        }

        String combinedText = StringUtils.hasText(pdfText.text())
                ? pdfText.text() + "\n\n" + ocrText
                : ocrText;
        ExtractionMethod method = StringUtils.hasText(pdfText.text()) ? ExtractionMethod.PDF_TEXT_AND_OCR : ExtractionMethod.OCR;
        return new TextExtractionResult(combinedText, method, pdfTextExtractorService.calculateTextQualityScore(combinedText));
    }

    private DocumentExtractionStrategy selectStrategy(DocumentTypeDetectionResult detection) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(detection.documentType(), detection.subtype()))
                .sorted(Comparator.comparing(strategy -> strategy instanceof GenericExtractionStrategy))
                .findFirst()
                .orElse(genericExtractionStrategy);
    }

    private NormalizedExtractionResult normalizeAndValidate(DocumentExtractionStrategy strategy, JsonNode rawData) {
        double llmConfidence = extractLlmConfidence(rawData);
        JsonNode normalized = strategy.normalize(rawData);
        if (normalized instanceof ObjectNode objectNode) {
            objectNode.remove("llmConfidence");
        }
        List<String> warnings = new ArrayList<>(strategy.validate(normalized));
        return new NormalizedExtractionResult(normalized, warnings, llmConfidence);
    }

    private JsonNode unwrapExtractedData(JsonNode modelData) {
        if (modelData != null && modelData.has("extractedData") && modelData.get("extractedData").isObject()) {
            return modelData.get("extractedData");
        }
        return modelData;
    }

    private double extractLlmConfidence(JsonNode data) {
        if (data != null && data.has("llmConfidence") && data.get("llmConfidence").isNumber()) {
            return data.get("llmConfidence").asDouble();
        }
        return 0.5;
    }

    private void validateRequest(ParserExtractionCommand command) {
        if (command == null) {
            throw new ParserException(ParserErrorCode.INVALID_REQUEST, "Request is required");
        }
        if (!StringUtils.hasText(command.documentId())) {
            throw new ParserException(ParserErrorCode.INVALID_REQUEST, "documentId is required");
        }
        if (!StringUtils.hasText(command.vehicleId())) {
            throw new ParserException(ParserErrorCode.INVALID_REQUEST, "vehicleId is required");
        }
        MultipartFile file = command.file();
        if (file == null || file.isEmpty()) {
            throw new ParserException(ParserErrorCode.INVALID_REQUEST, "file is required");
        }
        String filename = StringUtils.hasText(command.originalFileName())
                ? command.originalFileName()
                : file.getOriginalFilename();
        String contentType = file.getContentType();
        boolean pdfByName = filename != null && filename.toLowerCase().endsWith(".pdf");
        boolean pdfByContentType = contentType == null || contentType.equalsIgnoreCase("application/pdf");
        if (!pdfByName || !pdfByContentType) {
            throw new ParserException(ParserErrorCode.UNSUPPORTED_FILE_TYPE, "Only PDF files are supported");
        }
    }
}

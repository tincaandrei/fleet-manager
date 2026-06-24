package com.fleet.document.service;

import com.fleet.document.dto.ParserResultRequest;
import com.fleet.document.entity.DocumentExtractionDraft;
import com.fleet.document.entity.DocumentStatus;
import com.fleet.document.entity.DocumentType;
import com.fleet.document.entity.ParserStatus;
import com.fleet.document.entity.VehicleDocument;
import com.fleet.document.repository.DocumentExtractionDraftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DocumentParserResultService {

    private final DocumentExtractionDraftRepository extractionDraftRepository;

    public void applyParserResult(VehicleDocument document, ParserResultRequest request) {
        DocumentExtractionDraft draft = extractionDraftRepository.findByDocument(document)
                .orElseGet(() -> DocumentExtractionDraft.builder().document(document).build());
        applyParserMetadata(draft, request);

        if (request.parserStatus() == ParserStatus.PARSED && parserResultIsValid(request)) {
            document.setDocumentType(toDocumentType(request.detectedDocumentType()));
            document.setDocumentSubtype(normalizeText(request.detectedSubtype()));
            draft.setParserStatus(ParserStatus.PARSED);
            extractionDraftRepository.save(draft);
            document.setStatus(DocumentStatus.NEEDS_REVIEW);
        } else {
            draft.setParserStatus(request.parserStatus() == null ? ParserStatus.FAILED : request.parserStatus());
            if (draft.getParserStatus() == ParserStatus.PARSED) {
                draft.setErrorCode("INVALID_PARSER_RESULT");
                draft.setErrorMessage("Parsed result must include extracted data and confidence between 0 and 1 when present");
            }
            extractionDraftRepository.save(draft);
            document.setStatus(DocumentStatus.PARSING_FAILED);
        }
    }

    private void applyParserMetadata(DocumentExtractionDraft draft, ParserResultRequest request) {
        draft.setDetectedDocumentType(normalizeText(request.detectedDocumentType()));
        draft.setDetectedSubtype(normalizeText(request.detectedSubtype()));
        draft.setConfidence(request.confidence());
        draft.setExtractedData(request.extractedData());
        draft.setWarnings(request.warnings());
        draft.setParserName(normalizeText(request.parserName()));
        draft.setParserVersion(normalizeText(request.parserVersion()));
        draft.setExtractionMethod(request.extractionMethod());
        applyLlmUsage(draft, request);
        draft.setParserStatus(request.parserStatus());
        draft.setErrorCode(normalizeText(request.errorCode()));
        draft.setErrorMessage(normalizeText(request.errorMessage()));
    }

    private void applyLlmUsage(DocumentExtractionDraft draft, ParserResultRequest request) {
        if (request.llmUsage() == null) {
            draft.setLlmProvider(null);
            draft.setLlmModel(null);
            draft.setLlmRequestId(null);
            draft.setInputTokens(null);
            draft.setOutputTokens(null);
            draft.setTotalTokens(null);
            return;
        }
        draft.setLlmProvider(normalizeText(request.llmUsage().provider()));
        draft.setLlmModel(normalizeText(request.llmUsage().model()));
        draft.setLlmRequestId(normalizeText(request.llmUsage().requestId()));
        draft.setInputTokens(request.llmUsage().inputTokens());
        draft.setOutputTokens(request.llmUsage().outputTokens());
        draft.setTotalTokens(request.llmUsage().totalTokens());
    }

    private boolean parserResultIsValid(ParserResultRequest request) {
        return !CollectionUtils.isEmpty(request.extractedData()) && confidenceIsValid(request.confidence());
    }

    private boolean confidenceIsValid(BigDecimal confidence) {
        return confidence == null
                || (confidence.compareTo(BigDecimal.ZERO) >= 0 && confidence.compareTo(BigDecimal.ONE) <= 0);
    }

    private DocumentType toDocumentType(String value) {
        if (value == null || value.isBlank()) {
            return DocumentType.OTHER;
        }
        try {
            return DocumentType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return DocumentType.OTHER;
        }
    }

    private String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

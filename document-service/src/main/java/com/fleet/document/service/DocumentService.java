package com.fleet.document.service;

import com.fleet.document.dto.ApproveDocumentRequest;
import com.fleet.document.dto.ApprovedDocumentDataResponse;
import com.fleet.document.dto.DocumentExtractionResponse;
import com.fleet.document.dto.DocumentResponse;
import com.fleet.document.dto.ParserResultRequest;
import com.fleet.document.dto.RejectDocumentRequest;
import com.fleet.document.dto.ReviewDecision;
import com.fleet.document.dto.ReviewDocumentRequest;
import com.fleet.document.entity.ApprovedDataStatus;
import com.fleet.document.entity.ApprovedDocumentData;
import com.fleet.document.entity.DocumentExtractionDraft;
import com.fleet.document.entity.DocumentStatus;
import com.fleet.document.entity.DocumentType;
import com.fleet.document.entity.ParserStatus;
import com.fleet.document.entity.VehicleDocument;
import com.fleet.document.exception.ResourceNotFoundException;
import com.fleet.document.repository.ApprovedDocumentDataRepository;
import com.fleet.document.repository.DocumentExtractionDraftRepository;
import com.fleet.document.repository.VehicleDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final VehicleDocumentRepository documentRepository;
    private final DocumentExtractionDraftRepository extractionDraftRepository;
    private final ApprovedDocumentDataRepository approvedDataRepository;
    private final DocumentStorageService storageService;
    private final FleetVehicleClient fleetVehicleClient;

    private static final Set<DocumentStatus> REJECTABLE_STATUSES = EnumSet.of(
            DocumentStatus.NEEDS_REVIEW,
            DocumentStatus.PARSING_FAILED
    );

    @Transactional
    public DocumentResponse upload(MultipartFile file,
                                   Long vehicleId,
                                   DocumentType documentType,
                                   String authorizationHeader,
                                   Authentication authentication) {
        return uploadDocument(file, vehicleId, documentType, authorizationHeader, authentication);
    }

    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file,
                                           Long vehicleId,
                                           DocumentType declaredDocumentType,
                                           String authorizationHeader,
                                           Authentication authentication) {
        if (vehicleId == null) {
            throw new IllegalArgumentException("Vehicle id is required");
        }
        if (!fleetVehicleClient.vehicleExists(vehicleId, authorizationHeader)) {
            throw new IllegalArgumentException("Vehicle does not exist: " + vehicleId);
        }

        StoredDocumentFile storedFile = storageService.save(file);
        VehicleDocument document = VehicleDocument.builder()
                .vehicleId(vehicleId)
                .documentType(declaredDocumentType)
                .status(DocumentStatus.PARSING)
                .originalFileName(storedFile.originalFileName())
                .storedFileName(storedFile.storedFileName())
                .contentType(storedFile.contentType())
                .fileSize(storedFile.fileSize())
                .storagePath(storedFile.storagePath())
                .uploadedByUserId(SecurityUtils.currentUserId(authentication))
                .build();
        return toResponse(documentRepository.save(document), SecurityUtils.canReview(authentication));
    }

    @Transactional
    public DocumentResponse receiveParserResult(UUID id, ParserResultRequest request, Authentication authentication) {
        if (request.documentId() != null && !request.documentId().equals(id)) {
            throw new IllegalArgumentException("Document id in payload must match path document id");
        }

        VehicleDocument document = getDocument(id);
        if (document.getStatus() != DocumentStatus.PARSING && document.getStatus() != DocumentStatus.PARSING_FAILED) {
            throw new IllegalArgumentException("Parser results can only be received for PARSING documents");
        }

        DocumentExtractionDraft draft = extractionDraftRepository.findByDocument(document)
                .orElseGet(() -> DocumentExtractionDraft.builder().document(document).build());
        applyParserMetadata(draft, request);

        if (request.parserStatus() == ParserStatus.PARSED && parserResultIsValid(request)) {
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

        return toResponse(documentRepository.save(document), SecurityUtils.canReview(authentication));
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listByVehicle(Long vehicleId, Authentication authentication) {
        if (vehicleId == null) {
            throw new IllegalArgumentException("Vehicle id is required");
        }
        boolean reviewer = SecurityUtils.canReview(authentication);
        List<VehicleDocument> documents = reviewer
                ? documentRepository.findByVehicleIdOrderByCreatedAtDesc(vehicleId)
                : documentRepository.findByVehicleIdAndStatusOrderByCreatedAtDesc(vehicleId, DocumentStatus.VALIDATED);
        return documents.stream()
                .map(document -> toResponse(document, reviewer))
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse getById(UUID id, Authentication authentication) {
        return getDocument(id, authentication);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(UUID id, Authentication authentication) {
        VehicleDocument document = getDocument(id);
        boolean reviewer = SecurityUtils.canReview(authentication);
        if (!reviewer && document.getStatus() != DocumentStatus.VALIDATED) {
            throw new AccessDeniedException("Access denied");
        }
        return toResponse(document, reviewer);
    }

    @Transactional(readOnly = true)
    public Resource download(UUID id) {
        VehicleDocument document = getDocument(id);
        return storageService.load(document.getStoragePath());
    }

    @Transactional(readOnly = true)
    public VehicleDocument getDownloadMetadata(UUID id) {
        return getDocument(id);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> reviewQueue() {
        return reviewQueue(null);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> reviewQueue(Authentication authentication) {
        requireReviewer(authentication);
        return documentRepository.findByStatusOrderByCreatedAtAsc(DocumentStatus.NEEDS_REVIEW).stream()
                .map(document -> toResponse(document, true))
                .toList();
    }

    @Transactional
    public DocumentResponse review(UUID id, ReviewDocumentRequest request, Authentication authentication) {
        requireReviewer(authentication);
        if (request.decision() == ReviewDecision.APPROVE) {
            return approveDocument(id, new ApproveDocumentRequest(
                    request.approvedData(),
                    documentTypeFromReviewData(request.approvedData()),
                    subtypeFromReviewData(request.approvedData()),
                    null,
                    null
            ), authentication);
        }
        if (request.decision() == ReviewDecision.REJECT) {
            return rejectDocument(id, new RejectDocumentRequest(request.comment()), authentication);
        }
        throw new IllegalArgumentException("Invalid review decision");
    }

    @Transactional
    public DocumentResponse approveDocument(UUID id, ApproveDocumentRequest request, Authentication authentication) {
        requireReviewer(authentication);
        VehicleDocument document = getDocument(id);
        if (document.getStatus() != DocumentStatus.NEEDS_REVIEW) {
            throw new IllegalArgumentException("Only documents with NEEDS_REVIEW status can be approved");
        }
        if (CollectionUtils.isEmpty(request.approvedData())) {
            throw new IllegalArgumentException("Approved data is required when approving a document");
        }
        if (request.documentType() == null || request.documentType().isBlank()) {
            throw new IllegalArgumentException("Document type is required when approving a document");
        }

        ApprovedDocumentData approvedData = approvedDataRepository.findByDocument(document)
                .orElseGet(() -> ApprovedDocumentData.builder().document(document).build());
        approvedData.setVehicleId(document.getVehicleId());
        approvedData.setDocumentType(request.documentType().trim());
        approvedData.setSubtype(normalizeText(request.subtype()));
        approvedData.setApprovedData(request.approvedData());
        approvedData.setValidFrom(request.validFrom());
        approvedData.setValidUntil(request.validUntil());
        approvedData.setApprovedByUserId(SecurityUtils.currentUserId(authentication));
        approvedData.setApprovedAt(Instant.now());
        approvedData.setReviewComment(null);
        approvedData.setStatus(ApprovedDataStatus.ACTIVE);
        approvedDataRepository.save(approvedData);

        document.setStatus(DocumentStatus.VALIDATED);
        document.setApprovedData(approvedData);
        return toResponse(documentRepository.save(document), true);
    }

    @Transactional
    public DocumentResponse rejectDocument(UUID id, RejectDocumentRequest request, Authentication authentication) {
        requireReviewer(authentication);
        VehicleDocument document = getDocument(id);
        if (!REJECTABLE_STATUSES.contains(document.getStatus())) {
            throw new IllegalArgumentException("Only NEEDS_REVIEW or PARSING_FAILED documents can be rejected");
        }

        DocumentExtractionDraft draft = extractionDraftRepository.findByDocument(document)
                .orElseGet(() -> DocumentExtractionDraft.builder()
                        .document(document)
                        .parserStatus(ParserStatus.FAILED)
                        .build());
        draft.setErrorCode("REJECTED");
        draft.setErrorMessage(normalizeText(request == null ? null : request.reason()));
        if (draft.getParserStatus() == null) {
            draft.setParserStatus(ParserStatus.FAILED);
        }
        extractionDraftRepository.save(draft);

        document.setStatus(DocumentStatus.REJECTED);
        return toResponse(documentRepository.save(document), true);
    }

    @Transactional
    public DocumentResponse archive(UUID id) {
        return archiveDocument(id, null);
    }

    @Transactional
    public DocumentResponse archiveDocument(UUID id, Authentication authentication) {
        requireReviewer(authentication);
        VehicleDocument document = getDocument(id);
        document.setStatus(DocumentStatus.ARCHIVED);
        approvedDataRepository.findByDocument(document).ifPresent(approvedData -> {
            approvedData.setStatus(ApprovedDataStatus.ARCHIVED);
            approvedDataRepository.save(approvedData);
            document.setApprovedData(approvedData);
        });
        return toResponse(documentRepository.save(document), true);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getVehicleDocuments(Long vehicleId, Authentication authentication) {
        return listByVehicle(vehicleId, authentication);
    }

    @Transactional(readOnly = true)
    public List<ApprovedDocumentDataResponse> getApprovedVehicleDocumentData(Long vehicleId) {
        if (vehicleId == null) {
            throw new IllegalArgumentException("Vehicle id is required");
        }
        return approvedDataRepository.findByVehicleIdOrderByValidUntilAscCreatedAtDesc(vehicleId).stream()
                .map(this::toApprovedDocumentDataResponse)
                .toList();
    }

    private VehicleDocument getDocument(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
    }

    private void applyParserMetadata(DocumentExtractionDraft draft, ParserResultRequest request) {
        draft.setDetectedDocumentType(normalizeText(request.detectedDocumentType()));
        draft.setDetectedSubtype(normalizeText(request.detectedSubtype()));
        draft.setConfidence(request.confidence());
        draft.setExtractedData(request.extractedData());
        draft.setWarnings(request.warnings());
        draft.setParserName(normalizeText(request.parserName()));
        draft.setParserVersion(normalizeText(request.parserVersion()));
        draft.setParserStatus(request.parserStatus());
        draft.setErrorCode(normalizeText(request.errorCode()));
        draft.setErrorMessage(normalizeText(request.errorMessage()));
    }

    private boolean parserResultIsValid(ParserResultRequest request) {
        return !CollectionUtils.isEmpty(request.extractedData()) && confidenceIsValid(request.confidence());
    }

    private boolean confidenceIsValid(BigDecimal confidence) {
        return confidence == null
                || (confidence.compareTo(BigDecimal.ZERO) >= 0 && confidence.compareTo(BigDecimal.ONE) <= 0);
    }

    private void requireReviewer(Authentication authentication) {
        if (authentication != null && !SecurityUtils.canReview(authentication)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String documentTypeFromReviewData(java.util.Map<String, Object> approvedData) {
        if (CollectionUtils.isEmpty(approvedData)) {
            return null;
        }
        Object documentType = approvedData.get("documentType");
        return documentType == null ? null : documentType.toString();
    }

    private String subtypeFromReviewData(java.util.Map<String, Object> approvedData) {
        if (CollectionUtils.isEmpty(approvedData)) {
            return null;
        }
        Object subtype = approvedData.get("subtype");
        return subtype == null ? null : subtype.toString();
    }

    private DocumentResponse toResponse(VehicleDocument document, boolean includeExtractionDraft) {
        ApprovedDocumentData approvedData = approvedDataRepository.findByDocument(document).orElse(null);
        DocumentExtractionDraft extractionDraft = includeExtractionDraft
                ? extractionDraftRepository.findByDocument(document).orElse(null)
                : null;
        return new DocumentResponse(
                document.getId(),
                document.getVehicleId(),
                document.getDocumentType(),
                document.getStatus(),
                document.getOriginalFileName(),
                document.getStoredFileName(),
                document.getContentType(),
                document.getFileSize(),
                document.getStoragePath(),
                document.getUploadedByUserId(),
                document.getCreatedAt(),
                document.getUpdatedAt(),
                extractionDraft == null ? null : toDocumentExtractionResponse(extractionDraft),
                approvedData == null ? null : toApprovedDocumentDataResponse(approvedData)
        );
    }

    private DocumentExtractionResponse toDocumentExtractionResponse(DocumentExtractionDraft draft) {
        return new DocumentExtractionResponse(
                draft.getDetectedDocumentType(),
                draft.getDetectedSubtype(),
                draft.getConfidence(),
                draft.getExtractedData(),
                draft.getWarnings(),
                draft.getParserName(),
                draft.getParserVersion(),
                draft.getParserStatus(),
                draft.getErrorCode(),
                draft.getErrorMessage()
        );
    }

    private ApprovedDocumentDataResponse toApprovedDocumentDataResponse(ApprovedDocumentData approvedData) {
        return new ApprovedDocumentDataResponse(
                approvedData.getDocumentType(),
                approvedData.getSubtype(),
                approvedData.getValidFrom(),
                approvedData.getValidUntil(),
                approvedData.getApprovedData(),
                approvedData.getApprovedByUserId(),
                approvedData.getApprovedAt()
        );
    }
}

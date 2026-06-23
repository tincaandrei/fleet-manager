package com.fleet.document.service;

import com.fleet.document.dto.ApproveDocumentRequest;
import com.fleet.document.dto.ApprovedDocumentDataResponse;
import com.fleet.document.dto.DocumentExtractionResponse;
import com.fleet.document.dto.DocumentInfoFolderResponse;
import com.fleet.document.dto.DocumentResponse;
import com.fleet.document.dto.ParserResultRequest;
import com.fleet.document.dto.RejectDocumentRequest;
import com.fleet.document.dto.ReviewDecision;
import com.fleet.document.dto.ReviewDocumentRequest;
import com.fleet.document.dto.VehicleAlertGroupResponse;
import com.fleet.document.dto.VehicleBasicInfoResponse;
import com.fleet.document.dto.VehicleDocumentAttributeResponse;
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
import com.fleet.document.service.event.DocumentUploadedForParsingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final VehicleDocumentRepository documentRepository;
    private final DocumentExtractionDraftRepository extractionDraftRepository;
    private final ApprovedDocumentDataRepository approvedDataRepository;
    private final DocumentStorageService storageService;
    private final FleetVehicleClient fleetVehicleClient;
    private final DocumentParserResultService parserResultService;
    private final VehicleDocumentAttributeService vehicleDocumentAttributeService;
    private final ApplicationEventPublisher eventPublisher;

    private static final Set<DocumentStatus> REJECTABLE_STATUSES = EnumSet.of(
            DocumentStatus.NEEDS_REVIEW,
            DocumentStatus.PARSING_FAILED
    );

    @Transactional
    public DocumentResponse upload(MultipartFile file,
                                   Long vehicleId,
                                   String authorizationHeader,
                                   Authentication authentication) {
        return uploadDocument(file, vehicleId, authorizationHeader, authentication);
    }

    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file,
                                           Long vehicleId,
                                           String authorizationHeader,
                                           Authentication authentication) {
        if (vehicleId == null) {
            throw new IllegalArgumentException("Vehicle id is required");
        }
        VehicleBasicInfoResponse vehicle = fleetVehicleClient.vehicleBasicInfo(vehicleId, authorizationHeader);
        if (vehicle == null || vehicle.businessId() == null) {
            throw new IllegalArgumentException("Vehicle does not exist: " + vehicleId);
        }

        StoredDocumentFile storedFile = storageService.save(file);
        VehicleDocument document = VehicleDocument.builder()
                .vehicleId(vehicleId)
                .businessId(vehicle.businessId())
                .documentType(DocumentType.OTHER)
                .status(DocumentStatus.PARSING)
                .originalFileName(storedFile.originalFileName())
                .storedFileName(storedFile.storedFileName())
                .contentType(storedFile.contentType())
                .fileSize(storedFile.fileSize())
                .storagePath(storedFile.storagePath())
                .uploadedByUserId(SecurityUtils.currentUserId(authentication))
                .build();
        VehicleDocument savedDocument = documentRepository.save(document);
        eventPublisher.publishEvent(new DocumentUploadedForParsingEvent(savedDocument.getId()));
        return toResponse(savedDocument, SecurityUtils.canReview(authentication));
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

        parserResultService.applyParserResult(document, request);

        return toResponse(documentRepository.save(document), SecurityUtils.canReview(authentication));
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listByVehicle(Long vehicleId, String authorizationHeader, Authentication authentication) {
        if (vehicleId == null) {
            throw new IllegalArgumentException("Vehicle id is required");
        }
        assertVehicleVisible(vehicleId, authorizationHeader);
        boolean reviewer = SecurityUtils.canReview(authentication);
        Long businessId = SecurityUtils.currentBusinessId(authentication);
        List<VehicleDocument> documents = reviewer
                ? documentRepository.findByVehicleIdOrderByCreatedAtDesc(vehicleId)
                : documentRepository.findByVehicleIdAndStatusOrderByCreatedAtDesc(vehicleId, DocumentStatus.VALIDATED);
        return documents.stream()
                .filter(document -> SecurityUtils.isSuperadmin(authentication)
                        || businessId == null
                        || businessId.equals(document.getBusinessId()))
                .map(document -> toResponse(document, reviewer))
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse getById(UUID id, Authentication authentication) {
        return getDocument(id, authentication);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getById(UUID id, String authorizationHeader, Authentication authentication) {
        return getDocument(id, authorizationHeader, authentication);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(UUID id, Authentication authentication) {
        VehicleDocument document = getDocument(id);
        boolean reviewer = SecurityUtils.canReview(authentication);
        assertCanReadDocument(document, authentication);
        if (!reviewer && document.getStatus() != DocumentStatus.VALIDATED) {
            throw new AccessDeniedException("Access denied");
        }
        return toResponse(document, reviewer);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(UUID id, String authorizationHeader, Authentication authentication) {
        VehicleDocument document = getDocument(id);
        boolean reviewer = SecurityUtils.canReview(authentication);
        assertCanReadDocument(document, authentication, authorizationHeader);
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
    public Resource download(UUID id, String authorizationHeader, Authentication authentication) {
        VehicleDocument document = getDocument(id);
        assertCanReadDocument(document, authentication, authorizationHeader);
        return storageService.load(document.getStoragePath());
    }

    @Transactional(readOnly = true)
    public VehicleDocument getDownloadMetadata(UUID id) {
        return getDocument(id);
    }

    @Transactional(readOnly = true)
    public VehicleDocument getDownloadMetadata(UUID id, String authorizationHeader, Authentication authentication) {
        VehicleDocument document = getDocument(id);
        assertCanReadDocument(document, authentication, authorizationHeader);
        return document;
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> reviewQueue() {
        return reviewQueue(null);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> reviewQueue(Authentication authentication) {
        requireReviewer(authentication);
        Long businessId = SecurityUtils.currentBusinessId(authentication);
        List<VehicleDocument> queue = SecurityUtils.isSuperadmin(authentication)
                ? documentRepository.findByStatusOrderByCreatedAtAsc(DocumentStatus.NEEDS_REVIEW)
                : documentRepository.findByBusinessIdAndStatusOrderByCreatedAtAsc(businessId, DocumentStatus.NEEDS_REVIEW);
        return queue.stream()
                .map(document -> toResponse(document, true))
                .toList();
    }

    @Transactional
    public DocumentResponse review(UUID id, ReviewDocumentRequest request, Authentication authentication) {
        requireReviewer(authentication);
        if (request.decision() == ReviewDecision.APPROVE) {
            VehicleDocument document = getDocument(id);
            return approveDocument(id, new ApproveDocumentRequest(
                    request.approvedData(),
                    documentTypeFromReviewData(document, request.approvedData()),
                    subtypeFromReviewData(document, request.approvedData()),
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
        assertCanReviewDocument(document, authentication);
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
        LocalDate validFrom = firstDate(request.validFrom(), request.approvedData(), "validFrom", "startDate");
        LocalDate validUntil = firstDate(request.validUntil(), request.approvedData(), "validUntil", "expiryDate", "expirationDate");

        approvedData.setVehicleId(document.getVehicleId());
        approvedData.setBusinessId(document.getBusinessId());
        approvedData.setDocumentType(request.documentType().trim());
        approvedData.setSubtype(normalizeText(request.subtype()));
        approvedData.setApprovedData(request.approvedData());
        approvedData.setValidFrom(validFrom);
        approvedData.setValidUntil(validUntil);
        approvedData.setApprovedByUserId(SecurityUtils.currentUserId(authentication));
        approvedData.setApprovedAt(Instant.now());
        approvedData.setReviewComment(null);
        approvedData.setStatus(ApprovedDataStatus.ACTIVE);
        approvedDataRepository.save(approvedData);
        vehicleDocumentAttributeService.upsertFromApprovedData(approvedData);

        document.setStatus(DocumentStatus.VALIDATED);
        document.setApprovedData(approvedData);
        return toResponse(documentRepository.save(document), true);
    }

    @Transactional
    public DocumentResponse rejectDocument(UUID id, RejectDocumentRequest request, Authentication authentication) {
        requireReviewer(authentication);
        VehicleDocument document = getDocument(id);
        assertCanReviewDocument(document, authentication);
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
        assertCanReviewDocument(document, authentication);
        document.setStatus(DocumentStatus.ARCHIVED);
        approvedDataRepository.findByDocument(document).ifPresent(approvedData -> {
            approvedData.setStatus(ApprovedDataStatus.ARCHIVED);
            approvedDataRepository.save(approvedData);
            document.setApprovedData(approvedData);
        });
        vehicleDocumentAttributeService.archiveForDocument(document);
        return toResponse(documentRepository.save(document), true);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getVehicleDocuments(Long vehicleId, String authorizationHeader, Authentication authentication) {
        return listByVehicle(vehicleId, authorizationHeader, authentication);
    }

    @Transactional(readOnly = true)
    public List<ApprovedDocumentDataResponse> getApprovedVehicleDocumentData(
            Long vehicleId,
            String authorizationHeader,
            Authentication authentication
    ) {
        if (vehicleId == null) {
            throw new IllegalArgumentException("Vehicle id is required");
        }
        VehicleBasicInfoResponse vehicle = assertVehicleVisible(vehicleId, authorizationHeader);
        Long businessId = SecurityUtils.currentBusinessId(authentication);
        List<ApprovedDocumentData> data = SecurityUtils.isSuperadmin(authentication)
                ? approvedDataRepository.findByVehicleIdOrderByValidUntilAscCreatedAtDesc(vehicleId)
                : approvedDataRepository.findByVehicleIdAndBusinessIdOrderByValidUntilAscCreatedAtDesc(
                        vehicleId,
                        businessId == null ? vehicle.businessId() : businessId
                );
        return data.stream()
                .map(this::toApprovedDocumentDataResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentInfoFolderResponse getInfoFolder(UUID id, Authentication authentication) {
        VehicleDocument document = getDocument(id);
        assertCanReadDocument(document, authentication);
        return toInfoFolder(document);
    }

    @Transactional(readOnly = true)
    public DocumentInfoFolderResponse getInfoFolder(UUID id, String authorizationHeader, Authentication authentication) {
        VehicleDocument document = getDocument(id);
        assertCanReadDocument(document, authentication, authorizationHeader);
        return toInfoFolder(document);
    }

    private DocumentInfoFolderResponse toInfoFolder(VehicleDocument document) {
        ApprovedDocumentData approvedData = approvedDataRepository.findByDocument(document).orElse(null);
        DocumentExtractionDraft draft = extractionDraftRepository.findByDocument(document).orElse(null);
        Map<String, Object> source = approvedData != null ? approvedData.getApprovedData() : draft == null ? Map.of() : draft.getExtractedData();
        if (source == null) {
            source = Map.of();
        }
        Map<String, Object> canonical = source.entrySet().stream()
                .filter(entry -> Set.of("documentType", "subtype", "licensePlate", "vin", "validFrom", "validUntil", "expiryDate", "expirationDate").contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<String, Object> extra = source.entrySet().stream()
                .filter(entry -> !canonical.containsKey(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return new DocumentInfoFolderResponse(
                document.getId(),
                document.getVehicleId(),
                document.getBusinessId(),
                document.getDocumentType(),
                document.getDocumentSubtype(),
                document.getStatus(),
                document.getOriginalFileName(),
                canonical,
                extra
        );
    }

    @Transactional(readOnly = true)
    public List<com.fleet.document.dto.VehicleDocumentAttributeResponse> getVehicleDocumentAttributes(
            Long vehicleId,
            String authorizationHeader
    ) {
        if (vehicleId == null) {
            throw new IllegalArgumentException("Vehicle id is required");
        }
        assertVehicleVisible(vehicleId, authorizationHeader);
        return vehicleDocumentAttributeService.listActiveByVehicle(vehicleId);
    }

    @Transactional(readOnly = true)
    public List<com.fleet.document.dto.VehicleDocumentAttributeResponse> getExpiringVehicleDocumentAttributes(
            Integer days,
            boolean includeExpired,
            String authorizationHeader,
            Authentication authentication
    ) {
        requireReviewer(authentication);
        int effectiveDays = days == null ? 30 : Math.max(days, 0);
        List<Long> vehicleIds = fleetVehicleClient.activeVehicles(authorizationHeader).stream()
                .map(VehicleBasicInfoResponse::id)
                .toList();
        return vehicleDocumentAttributeService.listExpiringAttributesForVehicles(vehicleIds, effectiveDays, includeExpired);
    }

    @Transactional(readOnly = true)
    public List<VehicleAlertGroupResponse> getGroupedVehicleAlerts(
            Integer days,
            boolean includeExpired,
            String authorizationHeader
    ) {
        List<VehicleBasicInfoResponse> vehicles = fleetVehicleClient.activeVehicles(authorizationHeader);
        List<Long> vehicleIds = vehicles.stream().map(VehicleBasicInfoResponse::id).toList();
        List<VehicleDocumentAttributeResponse> alerts = vehicleDocumentAttributeService.listExpiringAttributesForVehicles(
                vehicleIds,
                days == null ? 30 : Math.max(days, 0),
                includeExpired
        );
        Map<Long, List<VehicleDocumentAttributeResponse>> byVehicle = alerts.stream()
                .collect(Collectors.groupingBy(VehicleDocumentAttributeResponse::vehicleId));
        return vehicles.stream()
                .map(vehicle -> new VehicleAlertGroupResponse(
                        vehicle.id(),
                        vehicle.businessId(),
                        vehicle.licensePlate(),
                        vehicle.brand(),
                        vehicle.model(),
                        vehicle.assignedUserId(),
                        vehicle.assignedDriverName(),
                        byVehicle.getOrDefault(vehicle.id(), List.of())
                ))
                .toList();
    }

    private VehicleDocument getDocument(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
    }

    private VehicleBasicInfoResponse assertVehicleVisible(Long vehicleId, String authorizationHeader) {
        VehicleBasicInfoResponse vehicle = fleetVehicleClient.vehicleBasicInfo(vehicleId, authorizationHeader);
        if (vehicle == null || vehicle.businessId() == null) {
            throw new AccessDeniedException("Access denied");
        }
        return vehicle;
    }

    private void assertCanReadDocument(VehicleDocument document, Authentication authentication) {
        if (SecurityUtils.isSuperadmin(authentication)) {
            return;
        }
        Long businessId = SecurityUtils.currentBusinessId(authentication);
        if (businessId != null && businessId.equals(document.getBusinessId())) {
            return;
        }
        throw new AccessDeniedException("Access denied");
    }

    private void assertCanReadDocument(VehicleDocument document, Authentication authentication, String authorizationHeader) {
        assertCanReadDocument(document, authentication);
        if (SecurityUtils.isEmployee(authentication)) {
            assertVehicleVisible(document.getVehicleId(), authorizationHeader);
        }
    }

    private void assertCanReviewDocument(VehicleDocument document, Authentication authentication) {
        if (SecurityUtils.isSuperadmin(authentication)) {
            return;
        }
        Long businessId = SecurityUtils.currentBusinessId(authentication);
        if (businessId != null && businessId.equals(document.getBusinessId())) {
            return;
        }
        throw new AccessDeniedException("Access denied");
    }

    private void requireReviewer(Authentication authentication) {
        if (authentication != null && !SecurityUtils.canReview(authentication)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String documentTypeFromReviewData(VehicleDocument document, java.util.Map<String, Object> approvedData) {
        if (CollectionUtils.isEmpty(approvedData)) {
            return document.getDocumentType() == null ? null : document.getDocumentType().name();
        }
        Object documentType = approvedData.get("documentType");
        if (documentType != null && !documentType.toString().isBlank()) {
            return documentType.toString();
        }
        return extractionDraftRepository.findByDocument(document)
                .map(DocumentExtractionDraft::getDetectedDocumentType)
                .filter(value -> !value.isBlank())
                .orElse(document.getDocumentType() == null ? null : document.getDocumentType().name());
    }

    private String subtypeFromReviewData(VehicleDocument document, java.util.Map<String, Object> approvedData) {
        if (!CollectionUtils.isEmpty(approvedData)) {
            Object subtype = approvedData.get("subtype");
            if (subtype != null && !subtype.toString().isBlank()) {
                return subtype.toString();
            }
        }
        return extractionDraftRepository.findByDocument(document)
                .map(DocumentExtractionDraft::getDetectedSubtype)
                .filter(value -> !value.isBlank())
                .orElse(null);
    }

    private LocalDate firstDate(LocalDate preferred, Map<String, Object> data, String... keys) {
        if (preferred != null) {
            return preferred;
        }
        if (CollectionUtils.isEmpty(data)) {
            return null;
        }
        for (String key : keys) {
            Object value = data.get(key);
            if (value == null || value.toString().isBlank()) {
                continue;
            }
            try {
                return LocalDate.parse(value.toString().trim());
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
        return null;
    }

    private DocumentResponse toResponse(VehicleDocument document, boolean includeExtractionDraft) {
        ApprovedDocumentData approvedData = approvedDataRepository.findByDocument(document).orElse(null);
        DocumentExtractionDraft extractionDraft = includeExtractionDraft
                ? extractionDraftRepository.findByDocument(document).orElse(null)
                : null;
        return new DocumentResponse(
                document.getId(),
                document.getVehicleId(),
                document.getBusinessId(),
                document.getDocumentType(),
                document.getDocumentSubtype(),
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

package com.fleet.document.service;

import com.fleet.document.dto.ApprovedDataResponse;
import com.fleet.document.dto.DocumentResponse;
import com.fleet.document.dto.ExtractionResponse;
import com.fleet.document.dto.ReviewDecision;
import com.fleet.document.dto.ReviewDocumentRequest;
import com.fleet.document.entity.ApprovedDataStatus;
import com.fleet.document.entity.ApprovedDocumentData;
import com.fleet.document.entity.DocumentExtraction;
import com.fleet.document.entity.DocumentStatus;
import com.fleet.document.entity.DocumentType;
import com.fleet.document.entity.ExtractionStatus;
import com.fleet.document.entity.VehicleDocument;
import com.fleet.document.exception.ResourceNotFoundException;
import com.fleet.document.repository.ApprovedDocumentDataRepository;
import com.fleet.document.repository.DocumentExtractionRepository;
import com.fleet.document.repository.VehicleDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final VehicleDocumentRepository documentRepository;
    private final DocumentExtractionRepository extractionRepository;
    private final ApprovedDocumentDataRepository approvedDataRepository;
    private final DocumentStorageService storageService;
    private final PythonDocumentParserService parserService;
    private final FleetVehicleClient fleetVehicleClient;

    @Transactional
    public DocumentResponse upload(MultipartFile file,
                                   Long vehicleId,
                                   DocumentType documentType,
                                   String authorizationHeader,
                                   Authentication authentication) {
        if (vehicleId == null) {
            throw new IllegalArgumentException("Vehicle id is required");
        }
        if (documentType == null) {
            throw new IllegalArgumentException("Document type is required");
        }
        if (!fleetVehicleClient.vehicleExists(vehicleId, authorizationHeader)) {
            throw new IllegalArgumentException("Vehicle does not exist: " + vehicleId);
        }

        StoredDocumentFile storedFile = storageService.save(file);
        VehicleDocument document = VehicleDocument.builder()
                .vehicleId(vehicleId)
                .documentType(documentType)
                .status(DocumentStatus.PARSING)
                .originalFileName(storedFile.originalFileName())
                .storedFileName(storedFile.storedFileName())
                .contentType(storedFile.contentType())
                .fileSize(storedFile.fileSize())
                .storagePath(storedFile.storagePath())
                .uploadedByUserId(SecurityUtils.currentUserId(authentication))
                .build();
        document = documentRepository.save(document);

        PythonParserResult parserResult = parserService.parse(storedFile.storagePath());
        DocumentExtraction extraction = DocumentExtraction.builder()
                .document(document)
                .extractionStatus(parserResult.parsed() ? ExtractionStatus.PARSED : ExtractionStatus.FAILED)
                .parserName(parserResult.parserName())
                .parserVersion(parserResult.parserVersion())
                .rawExtractedData(parserResult.data())
                .extractionConfidence(parserResult.confidence())
                .errorMessage(parserResult.errorMessage())
                .build();
        extractionRepository.save(extraction);
        document.setStatus(parserResult.parsed() ? DocumentStatus.NEEDS_REVIEW : DocumentStatus.FAILED_PARSING);
        document.setExtraction(extraction);
        return toResponse(documentRepository.save(document), true);
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
        return toResponse(getDocument(id), SecurityUtils.canReview(authentication));
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
        return documentRepository.findByStatusOrderByCreatedAtAsc(DocumentStatus.NEEDS_REVIEW).stream()
                .map(document -> toResponse(document, true))
                .toList();
    }

    @Transactional
    public DocumentResponse review(UUID id, ReviewDocumentRequest request, Authentication authentication) {
        VehicleDocument document = getDocument(id);
        if (document.getStatus() != DocumentStatus.NEEDS_REVIEW) {
            throw new IllegalArgumentException("Only documents with NEEDS_REVIEW status can be reviewed");
        }
        DocumentExtraction extraction = extractionRepository.findByDocument(document)
                .orElseThrow(() -> new ResourceNotFoundException("Extraction not found for document: " + id));

        if (request.decision() == ReviewDecision.APPROVE) {
            if (CollectionUtils.isEmpty(request.approvedData())) {
                throw new IllegalArgumentException("Approved data is required when approving a document");
            }
            ApprovedDocumentData approvedData = approvedDataRepository.findByDocument(document)
                    .orElseGet(() -> ApprovedDocumentData.builder().document(document).build());
            approvedData.setApprovedData(request.approvedData());
            approvedData.setApprovedByUserId(SecurityUtils.currentUserId(authentication));
            approvedData.setApprovedAt(Instant.now());
            approvedData.setReviewComment(normalizeComment(request.comment()));
            approvedData.setStatus(ApprovedDataStatus.ACTIVE);
            approvedDataRepository.save(approvedData);

            extraction.setExtractionStatus(ExtractionStatus.APPROVED);
            extractionRepository.save(extraction);
            document.setStatus(DocumentStatus.VALIDATED);
            document.setApprovedData(approvedData);
        } else if (request.decision() == ReviewDecision.REJECT) {
            extraction.setExtractionStatus(ExtractionStatus.REJECTED);
            extraction.setErrorMessage(normalizeComment(request.comment()));
            extractionRepository.save(extraction);
            document.setStatus(DocumentStatus.REJECTED);
        } else {
            throw new IllegalArgumentException("Invalid review decision");
        }

        document.setExtraction(extraction);
        return toResponse(documentRepository.save(document), true);
    }

    @Transactional
    public DocumentResponse archive(UUID id) {
        VehicleDocument document = getDocument(id);
        document.setStatus(DocumentStatus.ARCHIVED);
        approvedDataRepository.findByDocument(document).ifPresent(approvedData -> {
            approvedData.setStatus(ApprovedDataStatus.ARCHIVED);
            approvedDataRepository.save(approvedData);
            document.setApprovedData(approvedData);
        });
        return toResponse(documentRepository.save(document), true);
    }

    private VehicleDocument getDocument(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));
    }

    private String normalizeComment(String comment) {
        return comment == null || comment.isBlank() ? null : comment.trim();
    }

    private DocumentResponse toResponse(VehicleDocument document, boolean includeExtraction) {
        ApprovedDocumentData approvedData = approvedDataRepository.findByDocument(document).orElse(null);
        DocumentExtraction extraction = includeExtraction
                ? extractionRepository.findByDocument(document).orElse(null)
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
                extraction == null ? null : toExtractionResponse(extraction),
                approvedData == null ? null : toApprovedDataResponse(approvedData)
        );
    }

    private ExtractionResponse toExtractionResponse(DocumentExtraction extraction) {
        return new ExtractionResponse(
                extraction.getId(),
                extraction.getExtractionStatus(),
                extraction.getParserName(),
                extraction.getParserVersion(),
                extraction.getRawExtractedData(),
                extraction.getExtractionConfidence(),
                extraction.getErrorMessage(),
                extraction.getCreatedAt(),
                extraction.getUpdatedAt()
        );
    }

    private ApprovedDataResponse toApprovedDataResponse(ApprovedDocumentData approvedData) {
        return new ApprovedDataResponse(
                approvedData.getId(),
                approvedData.getApprovedData(),
                approvedData.getApprovedByUserId(),
                approvedData.getApprovedAt(),
                approvedData.getReviewComment(),
                approvedData.getStatus(),
                approvedData.getCreatedAt(),
                approvedData.getUpdatedAt()
        );
    }
}

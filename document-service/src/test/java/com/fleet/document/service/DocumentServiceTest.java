package com.fleet.document.service;

import com.fleet.document.dto.ApproveDocumentRequest;
import com.fleet.document.dto.ParserResultRequest;
import com.fleet.document.dto.RejectDocumentRequest;
import com.fleet.document.dto.VehicleBasicInfoResponse;
import com.fleet.document.entity.ApprovedDocumentData;
import com.fleet.document.entity.DocumentExtractionDraft;
import com.fleet.document.entity.DocumentStatus;
import com.fleet.document.entity.DocumentType;
import com.fleet.document.entity.ParserStatus;
import com.fleet.document.entity.VehicleDocument;
import com.fleet.document.repository.ApprovedDocumentDataRepository;
import com.fleet.document.repository.DocumentExtractionDraftRepository;
import com.fleet.document.repository.VehicleDocumentRepository;
import com.fleet.document.service.event.DocumentUploadedForParsingEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private VehicleDocumentRepository documentRepository;

    @Mock
    private DocumentExtractionDraftRepository extractionDraftRepository;

    @Mock
    private ApprovedDocumentDataRepository approvedDataRepository;

    @Mock
    private DocumentStorageService storageService;

    @Mock
    private FleetVehicleClient fleetVehicleClient;

    @Mock
    private DocumentParserResultService parserResultService;

    @Mock
    private VehicleDocumentAttributeService vehicleDocumentAttributeService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private DocumentService documentService;

    private final Authentication staffAuth = new TestingAuthenticationToken(
            new JwtPrincipal("staff", 10L, 100L, java.util.Set.of("BUSINESS_ADMIN")),
            null,
            "ROLE_BUSINESS_ADMIN"
    );

    private final Authentication userAuth = new TestingAuthenticationToken(
            new JwtPrincipal("user", 20L, 100L, java.util.Set.of("EMPLOYEE")),
            null,
            "ROLE_EMPLOYEE"
    );

    @Test
    void uploadPdfCreatesParsingDocument() {
        when(fleetVehicleClient.vehicleBasicInfo(1L, "Bearer token")).thenReturn(vehicleInfo());
        when(storageService.save(multipartFile)).thenReturn(new StoredDocumentFile(
                "inspection.pdf",
                "stored.pdf",
                "application/pdf",
                123L,
                "/tmp/stored.pdf"
        ));
        when(documentRepository.save(any(VehicleDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(approvedDataRepository.findByDocument(any())).thenReturn(Optional.empty());
        when(extractionDraftRepository.findByDocument(any())).thenReturn(Optional.empty());

        documentService.uploadDocument(multipartFile, 1L, "Bearer token", staffAuth);

        ArgumentCaptor<VehicleDocument> captor = ArgumentCaptor.forClass(VehicleDocument.class);
        verify(documentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(DocumentStatus.PARSING);
        assertThat(captor.getValue().getVehicleId()).isEqualTo(1L);
        assertThat(captor.getValue().getUploadedByUserId()).isEqualTo(10L);

        ArgumentCaptor<DocumentUploadedForParsingEvent> eventCaptor = ArgumentCaptor.forClass(DocumentUploadedForParsingEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().documentId()).isEqualTo(captor.getValue().getId());
        verifyNoInteractions(parserResultService);
    }

    @Test
    void uploadRejectsUnknownVehicle() {
        when(fleetVehicleClient.vehicleBasicInfo(99L, "Bearer token")).thenReturn(null);

        assertThatThrownBy(() -> documentService.uploadDocument(
                multipartFile,
                99L,
                "Bearer token",
                staffAuth
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vehicle does not exist");
    }

    @Test
    void validParserResultCreatesDraftAndNeedsReviewStatus() {
        UUID documentId = UUID.randomUUID();
        VehicleDocument document = document(documentId, DocumentStatus.PARSING);
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        doAnswer(invocation -> {
            VehicleDocument parsedDocument = invocation.getArgument(0);
            parsedDocument.setStatus(DocumentStatus.NEEDS_REVIEW);
            return null;
        }).when(parserResultService).applyParserResult(any(VehicleDocument.class), any(ParserResultRequest.class));
        when(documentRepository.save(any(VehicleDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(approvedDataRepository.findByDocument(any())).thenReturn(Optional.empty());
        when(extractionDraftRepository.findByDocument(any())).thenReturn(Optional.empty());

        documentService.receiveParserResult(documentId, new ParserResultRequest(
                documentId,
                ParserStatus.PARSED,
                "TECHNICAL_INSPECTION",
                "ITP",
                new BigDecimal("0.91"),
                "mock-parser",
                "1.0",
                Map.of("expiryDate", "2027-03-10"),
                java.util.List.of("low contrast stamp"),
                null,
                null
        ), staffAuth);

        verify(parserResultService).applyParserResult(any(VehicleDocument.class), any(ParserResultRequest.class));
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.NEEDS_REVIEW);
    }

    @Test
    void failedParserResultSetsParsingFailed() {
        UUID documentId = UUID.randomUUID();
        VehicleDocument document = document(documentId, DocumentStatus.PARSING);
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        doAnswer(invocation -> {
            VehicleDocument failedDocument = invocation.getArgument(0);
            failedDocument.setStatus(DocumentStatus.PARSING_FAILED);
            return null;
        }).when(parserResultService).applyParserResult(any(VehicleDocument.class), any(ParserResultRequest.class));
        when(documentRepository.save(any(VehicleDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(approvedDataRepository.findByDocument(any())).thenReturn(Optional.empty());
        when(extractionDraftRepository.findByDocument(any())).thenReturn(Optional.empty());

        documentService.receiveParserResult(documentId, new ParserResultRequest(
                documentId,
                ParserStatus.FAILED,
                null,
                null,
                null,
                "mock-parser",
                "1.0",
                null,
                null,
                "OCR_FAILED",
                "Could not read PDF"
        ), staffAuth);

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.PARSING_FAILED);
    }

    @Test
    void approveNeedsReviewDocumentCreatesApprovedDataAndValidates() {
        UUID documentId = UUID.randomUUID();
        VehicleDocument document = document(documentId, DocumentStatus.NEEDS_REVIEW);
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(approvedDataRepository.findByDocument(document)).thenReturn(Optional.empty());
        when(approvedDataRepository.save(any(ApprovedDocumentData.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(documentRepository.save(any(VehicleDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(extractionDraftRepository.findByDocument(any())).thenReturn(Optional.empty());

        documentService.approveDocument(documentId, new ApproveDocumentRequest(
                Map.of("expiryDate", "2027-03-10"),
                "TECHNICAL_INSPECTION",
                "ITP",
                null,
                java.time.LocalDate.of(2027, 3, 10)
        ), staffAuth);

        ArgumentCaptor<ApprovedDocumentData> approvedCaptor = ArgumentCaptor.forClass(ApprovedDocumentData.class);
        verify(approvedDataRepository).save(approvedCaptor.capture());
        assertThat(approvedCaptor.getValue().getVehicleId()).isEqualTo(1L);
        assertThat(approvedCaptor.getValue().getDocumentType()).isEqualTo("TECHNICAL_INSPECTION");
        assertThat(approvedCaptor.getValue().getValidUntil()).isEqualTo(java.time.LocalDate.of(2027, 3, 10));
        verify(vehicleDocumentAttributeService).upsertFromApprovedData(approvedCaptor.getValue());
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.VALIDATED);
    }

    @Test
    void approveNonReviewDocumentIsRejected() {
        UUID documentId = UUID.randomUUID();
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document(documentId, DocumentStatus.PARSING)));

        assertThatThrownBy(() -> documentService.approveDocument(documentId, new ApproveDocumentRequest(
                Map.of("expiryDate", "2027-03-10"),
                "TECHNICAL_INSPECTION",
                null,
                null,
                null
        ), staffAuth)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NEEDS_REVIEW");
    }

    @Test
    void rejectNeedsReviewDocumentSetsRejected() {
        UUID documentId = UUID.randomUUID();
        VehicleDocument document = document(documentId, DocumentStatus.NEEDS_REVIEW);
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(extractionDraftRepository.findByDocument(document)).thenReturn(Optional.empty());
        when(extractionDraftRepository.save(any(DocumentExtractionDraft.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(documentRepository.save(any(VehicleDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(approvedDataRepository.findByDocument(any())).thenReturn(Optional.empty());

        documentService.rejectDocument(documentId, new RejectDocumentRequest("Not the right vehicle"), staffAuth);

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.REJECTED);
    }

    @Test
    void rejectParsingFailedDocumentSetsRejected() {
        UUID documentId = UUID.randomUUID();
        VehicleDocument document = document(documentId, DocumentStatus.PARSING_FAILED);
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(extractionDraftRepository.findByDocument(document)).thenReturn(Optional.empty());
        when(extractionDraftRepository.save(any(DocumentExtractionDraft.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(documentRepository.save(any(VehicleDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(approvedDataRepository.findByDocument(any())).thenReturn(Optional.empty());

        documentService.rejectDocument(documentId, new RejectDocumentRequest("Unreadable"), staffAuth);

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.REJECTED);
    }

    @Test
    void staffCanSeeExtractionDraft() {
        UUID documentId = UUID.randomUUID();
        VehicleDocument document = document(documentId, DocumentStatus.NEEDS_REVIEW);
        DocumentExtractionDraft draft = DocumentExtractionDraft.builder()
                .document(document)
                .parserStatus(ParserStatus.PARSED)
                .extractedData(Map.of("expiryDate", "2027-03-10"))
                .build();
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(approvedDataRepository.findByDocument(document)).thenReturn(Optional.empty());
        when(extractionDraftRepository.findByDocument(document)).thenReturn(Optional.of(draft));

        assertThat(documentService.getDocument(documentId, staffAuth).extraction()).isNotNull();
    }

    @Test
    void userCannotSeeUnvalidatedDocument() {
        UUID documentId = UUID.randomUUID();
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document(documentId, DocumentStatus.NEEDS_REVIEW)));

        assertThatThrownBy(() -> documentService.getDocument(documentId, userAuth))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    private VehicleDocument document(UUID id, DocumentStatus status) {
        VehicleDocument document = new VehicleDocument();
        document.setId(id);
        document.setVehicleId(1L);
        document.setBusinessId(100L);
        document.setDocumentType(DocumentType.TECHNICAL_INSPECTION);
        document.setStatus(status);
        document.setOriginalFileName("inspection.pdf");
        document.setStoredFileName("stored.pdf");
        document.setContentType("application/pdf");
        document.setFileSize(123L);
        document.setStoragePath("/tmp/stored.pdf");
        return document;
    }

    private VehicleBasicInfoResponse vehicleInfo() {
        return new VehicleBasicInfoResponse(1L, 100L, "B123ABC", "Dacia", "Logan", "ACTIVE", null, null);
    }
}

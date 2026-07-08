package com.fleet.document.service;

import com.fleet.document.dto.ApproveDocumentRequest;
import com.fleet.document.dto.DocumentHistoryResponse;
import com.fleet.document.dto.PagedResponse;
import com.fleet.document.dto.ParserResultRequest;
import com.fleet.document.dto.RejectDocumentRequest;
import com.fleet.document.dto.UserLookupResponse;
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
import com.fleet.document.messaging.DocumentParsingMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
    private AuthUserLookupClient authUserLookupClient;

    @Mock
    private DocumentParserResultService parserResultService;

    @Mock
    private DocumentHistoryPdfExportService historyPdfExportService;

    @Mock
    private VehicleDocumentAttributeService vehicleDocumentAttributeService;

    @Mock
    private DocumentParsingOutboxService parsingOutboxService;

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

    private final Authentication superadminAuth = new TestingAuthenticationToken(
            new JwtPrincipal("super", 1L, null, java.util.Set.of("SUPERADMIN")),
            null,
            "ROLE_SUPERADMIN"
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
        when(authUserLookupClient.lookupBusinessAdmins(100L, "Bearer token")).thenReturn(List.of(
                new UserLookupResponse(11L, "admin", "admin@example.com", 100L, "BUSINESS_ADMIN")
        ));
        when(approvedDataRepository.findByDocument(any())).thenReturn(Optional.empty());
        when(extractionDraftRepository.findByDocument(any())).thenReturn(Optional.empty());

        documentService.uploadDocument(multipartFile, 1L, "Bearer token", staffAuth);

        ArgumentCaptor<VehicleDocument> captor = ArgumentCaptor.forClass(VehicleDocument.class);
        verify(documentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(DocumentStatus.PARSING);
        assertThat(captor.getValue().getVehicleId()).isEqualTo(1L);
        assertThat(captor.getValue().getUploadedByUserId()).isEqualTo(10L);

        ArgumentCaptor<DocumentParsingMessage> messageCaptor = ArgumentCaptor.forClass(DocumentParsingMessage.class);
        verify(parsingOutboxService).enqueue(messageCaptor.capture());
        assertThat(messageCaptor.getValue().documentId()).isEqualTo(captor.getValue().getId());
        assertThat(messageCaptor.getValue().vehicleLabel()).isEqualTo("B123ABC");
        assertThat(messageCaptor.getValue().adminUserIds()).containsExactly(11L);
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
                null,
                null,
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

    @Test
    void employeeHistoryContainsOnlyOwnUploads() {
        VehicleDocument document = document(UUID.randomUUID(), DocumentStatus.PARSING);
        document.setUploadedByUserId(20L);
        when(documentRepository.findByUploadedByUserIdOrderByCreatedAtDesc(20L, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(document), PageRequest.of(0, 20), 1));
        when(authUserLookupClient.lookupUsers(List.of(20L), "Bearer token"))
                .thenReturn(List.of(new UserLookupResponse(20L, "user", "user@example.com", 100L, "EMPLOYEE")));

        PagedResponse<?> response = documentService.history(0, 20, null, "Bearer token", userAuth);

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.items()).hasSize(1);
        verify(documentRepository).findByUploadedByUserIdOrderByCreatedAtDesc(20L, PageRequest.of(0, 20));
    }

    @Test
    void businessAdminHistoryContainsOrganizationUploads() {
        VehicleDocument document = document(UUID.randomUUID(), DocumentStatus.NEEDS_REVIEW);
        document.setUploadedByUserId(30L);
        when(documentRepository.findByBusinessIdOrderByCreatedAtDesc(100L, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(document), PageRequest.of(0, 20), 1));
        when(authUserLookupClient.lookupUsers(List.of(30L), "Bearer token")).thenReturn(List.of());

        PagedResponse<?> response = documentService.history(0, 20, null, "Bearer token", staffAuth);

        assertThat(response.totalElements()).isEqualTo(1);
        verify(documentRepository).findByBusinessIdOrderByCreatedAtDesc(100L, PageRequest.of(0, 20));
    }

    @Test
    void businessAdminCanExportOrganizationHistoryPdf() {
        VehicleDocument document = document(UUID.randomUUID(), DocumentStatus.NEEDS_REVIEW);
        document.setUploadedByUserId(30L);
        when(documentRepository.findByBusinessIdOrderByCreatedAtDesc(100L)).thenReturn(List.of(document));
        when(authUserLookupClient.lookupUsers(List.of(30L), "Bearer token"))
                .thenReturn(List.of(new UserLookupResponse(30L, "driver", "driver@example.com", 100L, "EMPLOYEE")));
        when(historyPdfExportService.export(any(), any(), anyBoolean())).thenReturn(new byte[]{1, 2, 3});

        byte[] pdf = documentService.exportHistoryPdf(null, "Bearer token", staffAuth);

        assertThat(pdf).containsExactly(1, 2, 3);
        verify(documentRepository).findByBusinessIdOrderByCreatedAtDesc(100L);
        verify(historyPdfExportService).export(
                org.mockito.ArgumentMatchers.eq("Organization Document History"),
                org.mockito.ArgumentMatchers.<List<DocumentHistoryResponse>>argThat(historyItems -> historyItems.size() == 1
                        && historyItems.get(0).uploadedByUsername().equals("driver")),
                org.mockito.ArgumentMatchers.eq(false)
        );
    }

    @Test
    void superadminHistoryContainsAllUploads() {
        VehicleDocument document = document(UUID.randomUUID(), DocumentStatus.VALIDATED);
        when(documentRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(document), PageRequest.of(0, 20), 1));
        when(authUserLookupClient.lookupUsers(List.of(10L), "Bearer token")).thenReturn(List.of());

        PagedResponse<?> response = documentService.history(0, 20, null, "Bearer token", superadminAuth);

        assertThat(response.totalElements()).isEqualTo(1);
        verify(documentRepository).findAllByOrderByCreatedAtDesc(PageRequest.of(0, 20));
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
        document.setUploadedByUserId(10L);
        document.setCreatedAt(Instant.parse("2026-06-24T10:00:00Z"));
        document.setUpdatedAt(Instant.parse("2026-06-24T10:00:00Z"));
        return document;
    }

    private VehicleBasicInfoResponse vehicleInfo() {
        return new VehicleBasicInfoResponse(1L, 100L, "B123ABC", "Dacia", "Logan", "ACTIVE", null, null);
    }
}

package com.fleet.document.service;

import com.fleet.document.dto.UserLookupResponse;
import com.fleet.document.entity.ApprovedDataStatus;
import com.fleet.document.entity.NotificationType;
import com.fleet.document.entity.VehicleDocument;
import com.fleet.document.entity.VehicleDocumentAttribute;
import com.fleet.document.repository.VehicleDocumentAttributeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentExpiryNotificationSchedulerTest {

    private static final String SERVICE_AUTH_HEADER = "Bearer service-token";

    @Mock
    private VehicleDocumentAttributeRepository attributeRepository;

    @Mock
    private UserNotificationService notificationService;

    @Mock
    private NotificationEmailService emailService;

    @Mock
    private AuthUserLookupClient authUserLookupClient;

    @Mock
    private ServiceAuthTokenService serviceAuthTokenService;

    @InjectMocks
    private DocumentExpiryNotificationScheduler scheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "daysAhead", 30);
    }

    @Test
    void notifiesBusinessAdminsAboutExpiringDocuments() {
        UUID documentId = UUID.randomUUID();
        LocalDate validUntil = LocalDate.now().plusDays(10);
        VehicleDocumentAttribute attribute = attribute(documentId, 100L, "B123ABC", "RCA", validUntil);
        when(attributeRepository.findByStatusAndValidUntilBetweenOrderByValidUntilAsc(
                eq(ApprovedDataStatus.ACTIVE), any(LocalDate.class), any(LocalDate.class)
        )).thenReturn(List.of(attribute));
        when(serviceAuthTokenService.mintServiceAuthorizationHeader()).thenReturn(SERVICE_AUTH_HEADER);
        when(authUserLookupClient.lookupBusinessAdmins(100L, SERVICE_AUTH_HEADER)).thenReturn(List.of(
                new UserLookupResponse(30L, "orgadmin", "admin@org.com", 100L, "BUSINESS_ADMIN")
        ));
        when(notificationService.hasNotification(30L, documentId, NotificationType.DOCUMENT_EXPIRING))
                .thenReturn(false);

        scheduler.notifyExpiringDocuments();

        verify(notificationService).notifyDocumentExpiring(
                eq(30L), eq(documentId), eq("B123ABC"), eq("RCA"), eq(validUntil), eq(10L)
        );
        verify(emailService).send(eq("admin@org.com"), anyString(), contains("B123ABC"));
    }

    @Test
    void skipsDocumentsAlreadyNotified() {
        UUID documentId = UUID.randomUUID();
        VehicleDocumentAttribute attribute = attribute(documentId, 100L, "B123ABC", "RCA", LocalDate.now().plusDays(5));
        when(attributeRepository.findByStatusAndValidUntilBetweenOrderByValidUntilAsc(
                eq(ApprovedDataStatus.ACTIVE), any(LocalDate.class), any(LocalDate.class)
        )).thenReturn(List.of(attribute));
        when(serviceAuthTokenService.mintServiceAuthorizationHeader()).thenReturn(SERVICE_AUTH_HEADER);
        when(authUserLookupClient.lookupBusinessAdmins(100L, SERVICE_AUTH_HEADER)).thenReturn(List.of(
                new UserLookupResponse(30L, "orgadmin", "admin@org.com", 100L, "BUSINESS_ADMIN")
        ));
        when(notificationService.hasNotification(30L, documentId, NotificationType.DOCUMENT_EXPIRING))
                .thenReturn(true);

        scheduler.notifyExpiringDocuments();

        verify(notificationService, never()).notifyDocumentExpiring(
                anyLong(), any(UUID.class), anyString(), anyString(), any(LocalDate.class), anyLong()
        );
        verify(emailService, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void doesNothingWhenNoDocumentsExpire() {
        when(attributeRepository.findByStatusAndValidUntilBetweenOrderByValidUntilAsc(
                eq(ApprovedDataStatus.ACTIVE), any(LocalDate.class), any(LocalDate.class)
        )).thenReturn(List.of());

        scheduler.notifyExpiringDocuments();

        verifyNoInteractions(notificationService, emailService, authUserLookupClient, serviceAuthTokenService);
    }

    private VehicleDocumentAttribute attribute(
            UUID documentId,
            Long businessId,
            String licensePlate,
            String documentType,
            LocalDate validUntil
    ) {
        return VehicleDocumentAttribute.builder()
                .id(UUID.randomUUID())
                .vehicleId(1L)
                .businessId(businessId)
                .document(VehicleDocument.builder().id(documentId).build())
                .documentType(documentType)
                .licensePlate(licensePlate)
                .validUntil(validUntil)
                .status(ApprovedDataStatus.ACTIVE)
                .build();
    }
}

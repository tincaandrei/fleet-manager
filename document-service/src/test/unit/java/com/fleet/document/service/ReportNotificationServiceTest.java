package com.fleet.document.service;

import com.fleet.document.dto.UserLookupResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportNotificationServiceTest {

    private static final String AUTH_HEADER = "Bearer token";

    @Mock
    private AuthUserLookupClient authUserLookupClient;

    @Mock
    private UserNotificationService notificationService;

    @Mock
    private NotificationEmailService emailService;

    @InjectMocks
    private ReportNotificationService reportNotificationService;

    private final Authentication superadmin = new TestingAuthenticationToken(
            new JwtPrincipal("admin", 1L, null, Set.of("SUPERADMIN")),
            null,
            "ROLE_SUPERADMIN"
    );

    private final Authentication businessAdmin = new TestingAuthenticationToken(
            new JwtPrincipal("orgadmin", 30L, 100L, Set.of("BUSINESS_ADMIN")),
            null,
            "ROLE_BUSINESS_ADMIN"
    );

    @Test
    void notifiesOrganizationAdminsForSuperadminScopedReport() {
        when(authUserLookupClient.lookupBusinessAdmins(100L, AUTH_HEADER)).thenReturn(List.of(
                new UserLookupResponse(30L, "orgadmin", "admin@org.com", 100L, "BUSINESS_ADMIN")
        ));

        reportNotificationService.notifyReportGenerated("Vehicle cost report (Excel)", 100L, AUTH_HEADER, superadmin);

        verify(notificationService).notifyReportGenerated(30L, "Vehicle cost report (Excel)", "your organization");
        verify(emailService).send(eq("admin@org.com"), anyString(), contains("Vehicle cost report (Excel)"));
    }

    @Test
    void usesOwnBusinessScopeForBusinessAdmin() {
        when(authUserLookupClient.lookupBusinessAdmins(100L, AUTH_HEADER)).thenReturn(List.of(
                new UserLookupResponse(30L, "orgadmin", "admin@org.com", 100L, "BUSINESS_ADMIN")
        ));

        reportNotificationService.notifyReportGenerated("Document history report (PDF)", null, AUTH_HEADER, businessAdmin);

        verify(notificationService).notifyReportGenerated(30L, "Document history report (PDF)", "your organization");
        verify(emailService).send(eq("admin@org.com"), anyString(), anyString());
    }

    @Test
    void skipsGlobalSuperadminReports() {
        reportNotificationService.notifyReportGenerated("Vehicle cost report (Excel)", null, AUTH_HEADER, superadmin);

        verifyNoInteractions(authUserLookupClient, notificationService, emailService);
    }

    @Test
    void notificationFailureDoesNotPropagate() {
        when(authUserLookupClient.lookupBusinessAdmins(100L, AUTH_HEADER))
                .thenThrow(new IllegalStateException("auth-service down"));

        assertThatCode(() -> reportNotificationService.notifyReportGenerated(
                "Vehicle cost report (Excel)", 100L, AUTH_HEADER, superadmin
        )).doesNotThrowAnyException();
    }
}

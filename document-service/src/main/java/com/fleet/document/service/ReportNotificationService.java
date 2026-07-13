package com.fleet.document.service;

import com.fleet.document.dto.UserLookupResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Notifies the admins assigned to an organization (in-app + email) after a
 * report covering that organization has been generated. Notification failures
 * are logged and never propagated, so they cannot break the report download.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportNotificationService {

    private final AuthUserLookupClient authUserLookupClient;
    private final UserNotificationService notificationService;
    private final NotificationEmailService emailService;

    public void notifyReportGenerated(
            String reportName,
            Long businessIdFilter,
            String authorizationHeader,
            Authentication authentication
    ) {
        try {
            Long scopeBusinessId = effectiveScope(businessIdFilter, authentication);
            if (scopeBusinessId == null) {
                // Global or employee-scoped exports have no assigned organization admin.
                return;
            }
            List<UserLookupResponse> admins =
                    authUserLookupClient.lookupBusinessAdmins(scopeBusinessId, authorizationHeader);
            for (UserLookupResponse admin : admins) {
                if (admin.userId() == null) {
                    continue;
                }
                notificationService.notifyReportGenerated(admin.userId(), reportName, "your organization");
                emailService.send(
                        admin.email(),
                        "DoccuFleet: " + reportName + " generated",
                        emailBody(reportName)
                );
            }
        } catch (RuntimeException exception) {
            log.warn("Could not notify organization admins about generated report '{}'", reportName, exception);
        }
    }

    private Long effectiveScope(Long businessIdFilter, Authentication authentication) {
        if (SecurityUtils.isSuperadmin(authentication)) {
            return businessIdFilter;
        }
        if (SecurityUtils.isBusinessAdmin(authentication)) {
            return SecurityUtils.currentBusinessId(authentication);
        }
        return null;
    }

    private String emailBody(String reportName) {
        return """
                Hello,

                A report has just been generated for your organization in DoccuFleet.

                Report: %s
                Generated on: %s

                You can generate and download reports anytime from the DoccuFleet console.

                This is an automated notification. Please do not reply to this email.
                """.formatted(reportName, LocalDate.now());
    }
}

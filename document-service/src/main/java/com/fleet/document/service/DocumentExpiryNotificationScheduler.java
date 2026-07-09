package com.fleet.document.service;

import com.fleet.document.dto.UserLookupResponse;
import com.fleet.document.entity.ApprovedDataStatus;
import com.fleet.document.entity.NotificationType;
import com.fleet.document.entity.VehicleDocumentAttribute;
import com.fleet.document.repository.VehicleDocumentAttributeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Daily job that warns organization admins (in-app + email) about approved
 * documents whose validity ends within the configured window. Each admin is
 * notified at most once per document, tracked through the notification table.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.expiry-alerts", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DocumentExpiryNotificationScheduler {

    private final VehicleDocumentAttributeRepository attributeRepository;
    private final UserNotificationService notificationService;
    private final NotificationEmailService emailService;
    private final AuthUserLookupClient authUserLookupClient;
    private final ServiceAuthTokenService serviceAuthTokenService;

    @Value("${app.expiry-alerts.days-ahead:30}")
    private int daysAhead;

    @Scheduled(cron = "${app.expiry-alerts.cron:0 0 7 * * *}")
    @Transactional
    public void notifyExpiringDocuments() {
        LocalDate today = LocalDate.now();
        List<VehicleDocumentAttribute> expiring = attributeRepository
                .findByStatusAndValidUntilBetweenOrderByValidUntilAsc(
                        ApprovedDataStatus.ACTIVE,
                        today,
                        today.plusDays(Math.max(daysAhead, 0))
                );
        if (expiring.isEmpty()) {
            return;
        }

        Map<Long, List<VehicleDocumentAttribute>> byBusiness = new LinkedHashMap<>();
        for (VehicleDocumentAttribute attribute : expiring) {
            if (attribute.getBusinessId() != null && attribute.getDocument() != null) {
                byBusiness.computeIfAbsent(attribute.getBusinessId(), ignored -> new ArrayList<>()).add(attribute);
            }
        }

        String serviceAuthorizationHeader = serviceAuthTokenService.mintServiceAuthorizationHeader();
        byBusiness.forEach((businessId, attributes) -> {
            try {
                notifyBusinessAdmins(businessId, attributes, today, serviceAuthorizationHeader);
            } catch (RuntimeException exception) {
                log.warn("Could not send expiry notifications for business {}", businessId, exception);
            }
        });
    }

    private void notifyBusinessAdmins(
            Long businessId,
            List<VehicleDocumentAttribute> attributes,
            LocalDate today,
            String serviceAuthorizationHeader
    ) {
        List<UserLookupResponse> admins =
                authUserLookupClient.lookupBusinessAdmins(businessId, serviceAuthorizationHeader);
        for (UserLookupResponse admin : admins) {
            if (admin.userId() == null) {
                continue;
            }
            List<String> emailLines = new ArrayList<>();
            for (VehicleDocumentAttribute attribute : attributes) {
                UUID documentId = attribute.getDocument().getId();
                if (notificationService.hasNotification(admin.userId(), documentId, NotificationType.DOCUMENT_EXPIRING)) {
                    continue;
                }
                long daysLeft = ChronoUnit.DAYS.between(today, attribute.getValidUntil());
                notificationService.notifyDocumentExpiring(
                        admin.userId(),
                        documentId,
                        vehicleLabel(attribute),
                        documentLabel(attribute),
                        attribute.getValidUntil(),
                        daysLeft
                );
                emailLines.add("- %s for vehicle %s expires on %s (%d %s left)".formatted(
                        documentLabel(attribute),
                        vehicleLabel(attribute),
                        attribute.getValidUntil(),
                        daysLeft,
                        daysLeft == 1 ? "day" : "days"
                ));
            }
            if (!emailLines.isEmpty()) {
                emailService.send(
                        admin.email(),
                        "DoccuFleet: documents expiring soon",
                        emailBody(emailLines)
                );
            }
        }
    }

    private String vehicleLabel(VehicleDocumentAttribute attribute) {
        if (StringUtils.hasText(attribute.getLicensePlate())) {
            return attribute.getLicensePlate().trim();
        }
        return "#" + attribute.getVehicleId();
    }

    private String documentLabel(VehicleDocumentAttribute attribute) {
        String type = StringUtils.hasText(attribute.getDocumentType()) ? attribute.getDocumentType().trim() : "Document";
        if (StringUtils.hasText(attribute.getSubtype())) {
            return type + " (" + attribute.getSubtype().trim() + ")";
        }
        return type;
    }

    private String emailBody(List<String> lines) {
        return """
                Hello,

                The following documents in your DoccuFleet fleet are approaching their expiry date:

                %s

                Please renew them and upload the new documents to keep your fleet compliant.

                This is an automated notification. Please do not reply to this email.
                """.formatted(String.join("\n", lines));
    }
}

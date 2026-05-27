package com.fleet.document.service;

import com.fleet.document.dto.VehicleDocumentAttributeResponse;
import com.fleet.document.entity.ApprovedDataStatus;
import com.fleet.document.entity.ApprovedDocumentData;
import com.fleet.document.entity.VehicleDocument;
import com.fleet.document.entity.VehicleDocumentAttribute;
import com.fleet.document.repository.VehicleDocumentAttributeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class VehicleDocumentAttributeService {

    private final VehicleDocumentAttributeRepository attributeRepository;

    @Transactional
    public VehicleDocumentAttribute upsertFromApprovedData(ApprovedDocumentData approvedData) {
        VehicleDocument document = approvedData.getDocument();
        String documentType = normalizeText(approvedData.getDocumentType());
        String subtype = normalizeText(approvedData.getSubtype());

        supersedePreviousActiveAttributes(approvedData, documentType, subtype);

        VehicleDocumentAttribute attribute = attributeRepository.findByDocument(document)
                .orElseGet(() -> VehicleDocumentAttribute.builder()
                        .document(document)
                        .approvedData(approvedData)
                        .build());

        attribute.setVehicleId(approvedData.getVehicleId());
        attribute.setBusinessId(approvedData.getBusinessId());
        attribute.setApprovedData(approvedData);
        attribute.setDocumentType(documentType);
        attribute.setSubtype(subtype);
        attribute.setLicensePlate(normalizeIdentifier(firstText(approvedData.getApprovedData(), "licensePlate", "registrationNumber")));
        attribute.setVin(normalizeIdentifier(firstText(approvedData.getApprovedData(), "vin", "vehicleIdentificationNumber")));
        attribute.setValidFrom(firstDate(approvedData.getValidFrom(), approvedData.getApprovedData(), "validFrom", "startDate"));
        attribute.setValidUntil(firstDate(approvedData.getValidUntil(), approvedData.getApprovedData(), "validUntil", "expiryDate", "expirationDate"));
        attribute.setSourceData(approvedData.getApprovedData());
        attribute.setStatus(ApprovedDataStatus.ACTIVE);
        return attributeRepository.save(attribute);
    }

    @Transactional
    public void archiveForDocument(VehicleDocument document) {
        attributeRepository.findByDocument(document).ifPresent(attribute -> {
            attribute.setStatus(ApprovedDataStatus.ARCHIVED);
            attributeRepository.save(attribute);
        });
    }

    @Transactional(readOnly = true)
    public List<VehicleDocumentAttributeResponse> listActiveByVehicle(Long vehicleId) {
        return attributeRepository.findByVehicleIdAndStatusOrderByValidUntilAscCreatedAtDesc(vehicleId, ApprovedDataStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VehicleDocumentAttributeResponse> listActiveByVehicleIds(List<Long> vehicleIds) {
        if (vehicleIds == null || vehicleIds.isEmpty()) {
            return List.of();
        }
        return attributeRepository.findByVehicleIdInAndStatusOrderByValidUntilAscCreatedAtDesc(vehicleIds, ApprovedDataStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VehicleDocumentAttributeResponse> listExpiringAttributes(int days, boolean includeExpired) {
        LocalDate today = LocalDate.now();
        LocalDate until = today.plusDays(Math.max(days, 0));
        List<VehicleDocumentAttribute> upcoming = attributeRepository
                .findByStatusAndValidUntilBetweenOrderByValidUntilAsc(ApprovedDataStatus.ACTIVE, today, until);

        if (!includeExpired) {
            return upcoming.stream().map(this::toResponse).toList();
        }

        List<VehicleDocumentAttribute> expired = attributeRepository
                .findByStatusAndValidUntilBeforeOrderByValidUntilAsc(ApprovedDataStatus.ACTIVE, today);
        return java.util.stream.Stream.concat(expired.stream(), upcoming.stream())
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VehicleDocumentAttributeResponse> listExpiringAttributesForVehicles(List<Long> vehicleIds, int days, boolean includeExpired) {
        if (vehicleIds == null || vehicleIds.isEmpty()) {
            return List.of();
        }
        LocalDate today = LocalDate.now();
        LocalDate until = today.plusDays(Math.max(days, 0));
        List<VehicleDocumentAttribute> upcoming = attributeRepository
                .findByVehicleIdInAndStatusAndValidUntilBetweenOrderByValidUntilAsc(vehicleIds, ApprovedDataStatus.ACTIVE, today, until);

        if (!includeExpired) {
            return upcoming.stream().map(this::toResponse).toList();
        }

        List<VehicleDocumentAttribute> expired = attributeRepository
                .findByVehicleIdInAndStatusAndValidUntilBeforeOrderByValidUntilAsc(vehicleIds, ApprovedDataStatus.ACTIVE, today);
        return java.util.stream.Stream.concat(expired.stream(), upcoming.stream())
                .map(this::toResponse)
                .toList();
    }

    public VehicleDocumentAttributeResponse toResponse(VehicleDocumentAttribute attribute) {
        return new VehicleDocumentAttributeResponse(
                attribute.getId(),
                attribute.getVehicleId(),
                attribute.getBusinessId(),
                attribute.getDocument().getId(),
                attribute.getApprovedData().getId(),
                attribute.getDocumentType(),
                attribute.getSubtype(),
                attribute.getLicensePlate(),
                attribute.getVin(),
                attribute.getValidFrom(),
                attribute.getValidUntil(),
                attribute.getSourceData(),
                attribute.getStatus(),
                attribute.getCreatedAt(),
                attribute.getUpdatedAt()
        );
    }

    private void supersedePreviousActiveAttributes(ApprovedDocumentData approvedData, String documentType, String subtype) {
        if (approvedData.getVehicleId() == null || documentType == null) {
            return;
        }
        attributeRepository.findByVehicleIdAndDocumentTypeAndStatus(
                        approvedData.getVehicleId(),
                        documentType,
                        ApprovedDataStatus.ACTIVE
                )
                .stream()
                .filter(attribute -> Objects.equals(normalizeText(attribute.getSubtype()), subtype))
                .filter(attribute -> !attribute.getDocument().getId().equals(approvedData.getDocument().getId()))
                .forEach(attribute -> {
                    attribute.setStatus(ApprovedDataStatus.SUPERSEDED);
                    attributeRepository.save(attribute);
                });
    }

    private LocalDate firstDate(LocalDate preferred, Map<String, Object> data, String... keys) {
        if (preferred != null) {
            return preferred;
        }
        String value = firstText(data, keys);
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String firstText(Map<String, Object> data, String... keys) {
        if (CollectionUtils.isEmpty(data)) {
            return null;
        }
        for (String key : keys) {
            Object value = data.get(key);
            if (value != null && !value.toString().isBlank()) {
                return value.toString().trim();
            }
        }
        return null;
    }

    private String normalizeIdentifier(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

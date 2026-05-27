package com.fleet.document.repository;

import com.fleet.document.entity.ApprovedDataStatus;
import com.fleet.document.entity.VehicleDocument;
import com.fleet.document.entity.VehicleDocumentAttribute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleDocumentAttributeRepository extends JpaRepository<VehicleDocumentAttribute, UUID> {

    Optional<VehicleDocumentAttribute> findByDocument(VehicleDocument document);

    List<VehicleDocumentAttribute> findByVehicleIdAndStatusOrderByValidUntilAscCreatedAtDesc(Long vehicleId, ApprovedDataStatus status);

    List<VehicleDocumentAttribute> findByVehicleIdInAndStatusOrderByValidUntilAscCreatedAtDesc(List<Long> vehicleIds, ApprovedDataStatus status);

    List<VehicleDocumentAttribute> findByVehicleIdAndDocumentTypeAndStatus(Long vehicleId, String documentType, ApprovedDataStatus status);

    List<VehicleDocumentAttribute> findByStatusAndValidUntilBetweenOrderByValidUntilAsc(
            ApprovedDataStatus status,
            LocalDate from,
            LocalDate until
    );

    List<VehicleDocumentAttribute> findByVehicleIdInAndStatusAndValidUntilBetweenOrderByValidUntilAsc(
            List<Long> vehicleIds,
            ApprovedDataStatus status,
            LocalDate from,
            LocalDate until
    );

    List<VehicleDocumentAttribute> findByStatusAndValidUntilBeforeOrderByValidUntilAsc(
            ApprovedDataStatus status,
            LocalDate date
    );

    List<VehicleDocumentAttribute> findByVehicleIdInAndStatusAndValidUntilBeforeOrderByValidUntilAsc(
            List<Long> vehicleIds,
            ApprovedDataStatus status,
            LocalDate date
    );
}

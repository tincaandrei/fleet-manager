package com.fleet.document.repository;

import com.fleet.document.entity.ApprovedDocumentData;
import com.fleet.document.entity.VehicleDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApprovedDocumentDataRepository extends JpaRepository<ApprovedDocumentData, UUID> {

    Optional<ApprovedDocumentData> findByDocument(VehicleDocument document);

    Optional<ApprovedDocumentData> findByDocumentId(UUID documentId);

    List<ApprovedDocumentData> findByVehicleIdOrderByValidUntilAscCreatedAtDesc(Long vehicleId);

    List<ApprovedDocumentData> findByValidUntilBefore(LocalDate date);
}

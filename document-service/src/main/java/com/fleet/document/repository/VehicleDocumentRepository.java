package com.fleet.document.repository;

import com.fleet.document.entity.DocumentStatus;
import com.fleet.document.entity.VehicleDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, UUID> {

    List<VehicleDocument> findByVehicleIdOrderByCreatedAtDesc(Long vehicleId);

    List<VehicleDocument> findByVehicleIdAndStatusOrderByCreatedAtDesc(Long vehicleId, DocumentStatus status);

    List<VehicleDocument> findByBusinessIdAndStatusOrderByCreatedAtAsc(Long businessId, DocumentStatus status);

    List<VehicleDocument> findByVehicleIdAndStatusInOrderByCreatedAtDesc(Long vehicleId, List<DocumentStatus> statuses);

    List<VehicleDocument> findByStatusOrderByCreatedAtAsc(DocumentStatus status);
}

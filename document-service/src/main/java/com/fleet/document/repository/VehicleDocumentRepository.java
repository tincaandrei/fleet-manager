package com.fleet.document.repository;

import com.fleet.document.entity.DocumentStatus;
import com.fleet.document.entity.VehicleDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, UUID> {

    Page<VehicleDocument> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<VehicleDocument> findByBusinessIdOrderByCreatedAtDesc(Long businessId, Pageable pageable);

    Page<VehicleDocument> findByUploadedByUserIdOrderByCreatedAtDesc(Long uploadedByUserId, Pageable pageable);

    List<VehicleDocument> findByVehicleIdOrderByCreatedAtDesc(Long vehicleId);

    List<VehicleDocument> findByVehicleIdAndStatusOrderByCreatedAtDesc(Long vehicleId, DocumentStatus status);

    List<VehicleDocument> findByBusinessIdAndStatusOrderByCreatedAtAsc(Long businessId, DocumentStatus status);

    List<VehicleDocument> findByVehicleIdAndStatusInOrderByCreatedAtDesc(Long vehicleId, List<DocumentStatus> statuses);

    List<VehicleDocument> findByStatusOrderByCreatedAtAsc(DocumentStatus status);
}

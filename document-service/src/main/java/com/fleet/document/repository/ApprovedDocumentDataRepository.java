package com.fleet.document.repository;

import com.fleet.document.entity.ApprovedDocumentData;
import com.fleet.document.entity.VehicleDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ApprovedDocumentDataRepository extends JpaRepository<ApprovedDocumentData, UUID> {

    Optional<ApprovedDocumentData> findByDocument(VehicleDocument document);
}

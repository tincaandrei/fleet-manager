package com.fleet.document.repository;

import com.fleet.document.entity.DocumentExtraction;
import com.fleet.document.entity.VehicleDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentExtractionRepository extends JpaRepository<DocumentExtraction, UUID> {

    Optional<DocumentExtraction> findByDocument(VehicleDocument document);
}

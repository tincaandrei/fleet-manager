package com.fleet.document.repository;

import com.fleet.document.entity.DocumentExtractionDraft;
import com.fleet.document.entity.VehicleDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentExtractionDraftRepository extends JpaRepository<DocumentExtractionDraft, UUID> {

    Optional<DocumentExtractionDraft> findByDocument(VehicleDocument document);

    Optional<DocumentExtractionDraft> findByDocumentId(UUID documentId);
}

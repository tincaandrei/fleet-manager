package com.fleet.document.repository;

import com.fleet.document.entity.DocumentParsingOutboxEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface DocumentParsingOutboxRepository extends JpaRepository<DocumentParsingOutboxEvent, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select event
            from DocumentParsingOutboxEvent event
            where event.publishedAt is null
            order by event.createdAt
            """)
    List<DocumentParsingOutboxEvent> findPendingForUpdate(Pageable pageable);
}

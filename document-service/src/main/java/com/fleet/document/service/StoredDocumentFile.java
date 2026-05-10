package com.fleet.document.service;

public record StoredDocumentFile(
        String originalFileName,
        String storedFileName,
        String contentType,
        long fileSize,
        String storagePath
) {
}

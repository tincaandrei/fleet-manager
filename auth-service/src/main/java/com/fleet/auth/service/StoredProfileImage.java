package com.fleet.auth.service;

public record StoredProfileImage(
        String originalFileName,
        String storedFileName,
        String contentType,
        long fileSize,
        String storagePath
) {
}

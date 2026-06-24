package com.fleet.fleet.service;

public record StoredVehicleImage(
        String originalFileName,
        String storedFileName,
        String contentType,
        long fileSize,
        String storagePath
) {
}

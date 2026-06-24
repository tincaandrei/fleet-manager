package com.fleet.auth.service;

import com.fleet.auth.config.ProfileImageStorageProperties;
import com.fleet.auth.exception.FileStorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileImageStorageService {

    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final Map<String, String> EXTENSIONS_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final ProfileImageStorageProperties storageProperties;

    public StoredProfileImage save(MultipartFile file) {
        validateImage(file);
        try {
            Path storageRoot = storageRoot();
            Files.createDirectories(storageRoot);

            String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() == null
                    ? "profile-image"
                    : file.getOriginalFilename());
            String contentType = normalizeContentType(file.getContentType(), originalFileName);
            String storedFileName = UUID.randomUUID() + EXTENSIONS_BY_CONTENT_TYPE.get(contentType);
            Path target = storageRoot.resolve(storedFileName).normalize();
            file.transferTo(target);

            return new StoredProfileImage(
                    originalFileName,
                    storedFileName,
                    contentType,
                    file.getSize(),
                    target.toString()
            );
        } catch (IOException ex) {
            throw new FileStorageException("Could not store profile image", ex);
        }
    }

    public Resource load(String storagePath) {
        try {
            Path path = Path.of(storagePath).normalize();
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new FileStorageException("Stored profile image is not readable", null);
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new FileStorageException("Stored profile image path is invalid", ex);
        }
    }

    public void deleteQuietly(String storagePath) {
        if (!StringUtils.hasText(storagePath)) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(storagePath).normalize());
        } catch (IOException ignored) {
            // Profile updates should still succeed if an old file cannot be removed.
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Profile image is required");
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("Profile image must be at most 5 MB");
        }
        String contentType = normalizeContentType(file.getContentType(), file.getOriginalFilename());
        if (!EXTENSIONS_BY_CONTENT_TYPE.containsKey(contentType)) {
            throw new IllegalArgumentException("Only JPG, PNG, or WebP profile images are accepted");
        }
    }

    private String normalizeContentType(String contentType, String originalFileName) {
        if (StringUtils.hasText(contentType)) {
            String normalized = contentType.toLowerCase(Locale.ROOT);
            if ("image/jpg".equals(normalized)) {
                return "image/jpeg";
            }
            if (EXTENSIONS_BY_CONTENT_TYPE.containsKey(normalized)) {
                return normalized;
            }
        }
        String lowerName = originalFileName == null ? "" : originalFileName.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lowerName.endsWith(".png")) {
            return "image/png";
        }
        if (lowerName.endsWith(".webp")) {
            return "image/webp";
        }
        return "";
    }

    private Path storageRoot() {
        return Path.of(storageProperties.getPath()).toAbsolutePath().normalize();
    }
}

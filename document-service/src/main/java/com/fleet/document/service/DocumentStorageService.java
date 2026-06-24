package com.fleet.document.service;

import com.fleet.document.config.DocumentStorageProperties;
import com.fleet.document.exception.FileStorageException;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentStorageService {

    private final DocumentStorageProperties storageProperties;

    public StoredDocumentFile save(MultipartFile file) {
        StoredFileType fileType = validateSupportedDocument(file);
        try {
            Path storageRoot = storageRoot();
            Files.createDirectories(storageRoot);

            String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() == null
                    ? "document" + fileType.extension()
                    : file.getOriginalFilename());
            String storedFileName = UUID.randomUUID() + fileType.extension();
            Path target = storageRoot.resolve(storedFileName).normalize();
            file.transferTo(target);

            return new StoredDocumentFile(
                    originalFileName,
                    storedFileName,
                    file.getContentType() == null ? fileType.contentType() : file.getContentType(),
                    file.getSize(),
                    target.toString()
            );
        } catch (IOException ex) {
            throw new FileStorageException("Could not store uploaded document", ex);
        }
    }

    public Resource load(String storagePath) {
        try {
            Path path = Path.of(storagePath).normalize();
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new FileStorageException("Stored document file is not readable", null);
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new FileStorageException("Stored document path is invalid", ex);
        }
    }

    private StoredFileType validateSupportedDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Document file is required");
        }
        String contentType = file.getContentType();
        String originalFileName = file.getOriginalFilename() == null
                ? ""
                : file.getOriginalFilename().toLowerCase(Locale.ROOT);

        if ("application/pdf".equalsIgnoreCase(contentType) || originalFileName.endsWith(".pdf")) {
            return new StoredFileType(".pdf", "application/pdf");
        }
        if ("image/jpeg".equalsIgnoreCase(contentType)
                || "image/jpg".equalsIgnoreCase(contentType)
                || originalFileName.endsWith(".jpg")
                || originalFileName.endsWith(".jpeg")) {
            return new StoredFileType(originalFileName.endsWith(".jpeg") ? ".jpeg" : ".jpg", "image/jpeg");
        }
        if ("image/png".equalsIgnoreCase(contentType) || originalFileName.endsWith(".png")) {
            return new StoredFileType(".png", "image/png");
        }

        throw new IllegalArgumentException("Only PDF, JPG, JPEG, or PNG documents are accepted");
    }

    private Path storageRoot() {
        return Path.of(storageProperties.getPath()).toAbsolutePath().normalize();
    }

    private record StoredFileType(String extension, String contentType) {
    }
}

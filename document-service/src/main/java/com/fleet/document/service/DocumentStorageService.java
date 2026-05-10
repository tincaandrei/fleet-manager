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
        validatePdf(file);
        try {
            Path storageRoot = storageRoot();
            Files.createDirectories(storageRoot);

            String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() == null
                    ? "document.pdf"
                    : file.getOriginalFilename());
            String storedFileName = UUID.randomUUID() + ".pdf";
            Path target = storageRoot.resolve(storedFileName).normalize();
            file.transferTo(target);

            return new StoredDocumentFile(
                    originalFileName,
                    storedFileName,
                    file.getContentType() == null ? "application/pdf" : file.getContentType(),
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

    private void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("PDF file is required");
        }
        String contentType = file.getContentType();
        String originalFileName = file.getOriginalFilename();
        boolean contentTypePdf = "application/pdf".equalsIgnoreCase(contentType);
        boolean extensionPdf = originalFileName != null
                && originalFileName.toLowerCase(Locale.ROOT).endsWith(".pdf");
        if (!contentTypePdf && !extensionPdf) {
            throw new IllegalArgumentException("Only PDF documents are accepted");
        }
    }

    private Path storageRoot() {
        return Path.of(storageProperties.getPath()).toAbsolutePath().normalize();
    }
}

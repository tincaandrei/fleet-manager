package com.fleet.document.service;

import com.fleet.document.config.DocumentStorageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentStorageServiceTest {

    @TempDir
    private Path storageRoot;

    @Test
    void invalidFileTypeIsRejected() {
        DocumentStorageProperties properties = new DocumentStorageProperties();
        properties.setPath(storageRoot.toString());
        DocumentStorageService storageService = new DocumentStorageService(properties);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.txt",
                "text/plain",
                "not a pdf".getBytes()
        );

        assertThatThrownBy(() -> storageService.save(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only PDF documents are accepted");
    }
}

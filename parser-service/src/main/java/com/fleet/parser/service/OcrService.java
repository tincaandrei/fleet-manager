package com.fleet.parser.service;

import org.springframework.web.multipart.MultipartFile;

public interface OcrService {

    boolean isEnabled();

    String extractText(MultipartFile file);
}

package com.fleet.parser.service;

import com.fleet.parser.config.OcrProperties;
import com.fleet.parser.dto.ParserErrorCode;
import com.fleet.parser.exception.ParserException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StubOcrService implements OcrService {

    private final OcrProperties properties;

    public StubOcrService(OcrProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    @Override
    public String extractText(MultipartFile file) {
        throw new ParserException(ParserErrorCode.OCR_FAILED, "OCR fallback is not implemented in parser-service v1");
    }
}

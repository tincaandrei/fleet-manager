package com.fleet.parser.exception;

import com.fleet.parser.dto.ParserErrorCode;

public class TextExtractionException extends ParserException {

    public TextExtractionException(String message) {
        super(ParserErrorCode.TEXT_EXTRACTION_FAILED, message);
    }

    public TextExtractionException(String message, Throwable cause) {
        super(ParserErrorCode.TEXT_EXTRACTION_FAILED, message, cause);
    }
}

package com.fleet.parser.exception;

import com.fleet.parser.dto.ParserErrorCode;

public class OllamaClientException extends ParserException {

    public OllamaClientException(ParserErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public OllamaClientException(ParserErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}

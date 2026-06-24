package com.fleet.document.service;

public class TextExtractionException extends RuntimeException {

    private final String errorCode;

    public TextExtractionException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public TextExtractionException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}

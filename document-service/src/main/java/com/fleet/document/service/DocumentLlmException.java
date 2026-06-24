package com.fleet.document.service;

public class DocumentLlmException extends RuntimeException {

    private final String errorCode;

    public DocumentLlmException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public DocumentLlmException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}

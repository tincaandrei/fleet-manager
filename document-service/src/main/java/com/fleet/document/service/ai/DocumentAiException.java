package com.fleet.document.service.ai;

public class DocumentAiException extends RuntimeException {

    private final String errorCode;

    public DocumentAiException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public DocumentAiException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}

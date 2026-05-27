package com.fleet.parser.exception;

import com.fleet.parser.dto.ParserErrorCode;

public class ParserException extends RuntimeException {

    private final ParserErrorCode errorCode;

    public ParserException(ParserErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ParserException(ParserErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ParserErrorCode getErrorCode() {
        return errorCode;
    }
}

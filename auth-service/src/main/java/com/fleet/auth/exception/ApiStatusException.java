package com.fleet.auth.exception;

import org.springframework.http.HttpStatus;

public class ApiStatusException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiStatusException(HttpStatus status, String code) {
        super(code);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}

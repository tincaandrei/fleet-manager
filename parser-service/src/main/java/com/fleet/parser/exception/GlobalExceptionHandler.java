package com.fleet.parser.exception;

import com.fleet.parser.config.ParserProperties;
import com.fleet.parser.dto.ParserErrorCode;
import com.fleet.parser.dto.ParserExtractionResponse;
import com.fleet.parser.dto.ParserStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ParserProperties parserProperties;

    public GlobalExceptionHandler(ParserProperties parserProperties) {
        this.parserProperties = parserProperties;
    }

    @ExceptionHandler({MissingServletRequestPartException.class, MaxUploadSizeExceededException.class})
    public ResponseEntity<ParserExtractionResponse> handleInvalidRequest(Exception exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(failure(
                request.getParameter("documentId"),
                ParserErrorCode.INVALID_REQUEST,
                exception.getMessage()
        ));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ParserExtractionResponse> handleResponseStatus(ResponseStatusException exception,
                                                                         HttpServletRequest request) {
        ParserErrorCode code = exception.getStatusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED)
                ? ParserErrorCode.INVALID_REQUEST
                : ParserErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(exception.getStatusCode()).body(failure(
                request.getParameter("documentId"),
                code,
                exception.getReason() == null ? exception.getMessage() : exception.getReason()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ParserExtractionResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        return ResponseEntity.internalServerError().body(failure(
                request.getParameter("documentId"),
                ParserErrorCode.INTERNAL_ERROR,
                "Unexpected parser-service error"
        ));
    }

    private ParserExtractionResponse failure(String documentId, ParserErrorCode code, String message) {
        return new ParserExtractionResponse(
                documentId,
                ParserStatus.FAILED,
                null,
                null,
                0.0,
                parserProperties.getName(),
                parserProperties.getVersion(),
                null,
                null,
                List.of(message),
                code,
                message
        );
    }
}

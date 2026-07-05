package com.fleet.document.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void externalServiceFailureReturnsBadGateway() {
        var response = handler.handleExternalService(
                new ExternalServiceException("Fleet Service is unavailable", new RuntimeException("connection failed"))
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Fleet Service is unavailable");
    }
}

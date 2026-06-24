package com.fleet.document.service;

import com.fleet.document.dto.UserLookupResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthUserLookupClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${auth.service.url}")
    private String authServiceUrl;

    public List<UserLookupResponse> lookupUsers(List<Long> userIds, String authorizationHeader) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        try {
            List<UserLookupResponse> response = restClientBuilder
                    .baseUrl(authServiceUrl)
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/users/lookup")
                            .queryParam("ids", userIds)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return response == null ? List.of() : response;
        } catch (RestClientException exception) {
            log.warn("Could not lookup upload users from auth-service", exception);
            return List.of();
        }
    }
}

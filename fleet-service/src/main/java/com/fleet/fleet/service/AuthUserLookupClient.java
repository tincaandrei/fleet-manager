package com.fleet.fleet.service;

import com.fleet.fleet.dto.AuthUserLookupResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthUserLookupClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${auth.service.url}")
    private String authServiceUrl;

    public Optional<AuthUserLookupResponse> lookupUser(Long userId, String authorizationHeader) {
        if (userId == null || !StringUtils.hasText(authorizationHeader)) {
            return Optional.empty();
        }

        try {
            List<AuthUserLookupResponse> response = restClientBuilder
                    .baseUrl(authServiceUrl)
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/users/lookup")
                            .queryParam("ids", userId)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (response == null) {
                return Optional.empty();
            }
            return response.stream()
                    .filter(user -> userId.equals(user.userId()))
                    .findFirst();
        } catch (RestClientException exception) {
            throw new IllegalArgumentException("Could not validate the selected driver", exception);
        }
    }
}

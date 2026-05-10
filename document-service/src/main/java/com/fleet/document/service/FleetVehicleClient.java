package com.fleet.document.service;

import com.fleet.document.dto.VehicleExistsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
public class FleetVehicleClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${fleet.service.url}")
    private String fleetServiceUrl;

    public boolean vehicleExists(Long vehicleId, String authorizationHeader) {
        try {
            VehicleExistsResponse response = restClientBuilder
                    .baseUrl(fleetServiceUrl)
                    .build()
                    .get()
                    .uri("/internal/vehicles/{id}/exists", vehicleId)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve()
                    .body(VehicleExistsResponse.class);
            return response != null && response.exists();
        } catch (RestClientException ex) {
            throw new IllegalArgumentException("Could not validate vehicle existence");
        }
    }
}

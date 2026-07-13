package com.fleet.document.service;

import com.fleet.document.dto.VehicleBasicInfoResponse;
import com.fleet.document.exception.ExternalServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FleetVehicleClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${fleet.service.url}")
    private String fleetServiceUrl;

    public VehicleBasicInfoResponse vehicleBasicInfo(Long vehicleId, String authorizationHeader) {
        try {
            return restClientBuilder
                    .baseUrl(fleetServiceUrl)
                    .build()
                    .get()
                    .uri("/internal/vehicles/{id}/basic-info", vehicleId)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve()
                    .body(VehicleBasicInfoResponse.class);
        } catch (RestClientException ex) {
            throw new IllegalArgumentException("Vehicle does not exist or is not visible");
        }
    }

    public List<VehicleBasicInfoResponse> activeVehicles(String authorizationHeader) {
        try {
            VehicleBasicInfoResponse[] response = restClientBuilder
                    .baseUrl(fleetServiceUrl)
                    .build()
                    .get()
                    .uri("/internal/vehicles/active")
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve()
                    .body(VehicleBasicInfoResponse[].class);
            return response == null ? List.of() : Arrays.asList(response);
        } catch (RestClientException ex) {
            throw new IllegalArgumentException("Could not load visible vehicles");
        }
    }

    public List<VehicleBasicInfoResponse> visibleVehicles(String authorizationHeader) {
        return visibleVehicles(authorizationHeader, null);
    }

    public List<VehicleBasicInfoResponse> visibleVehicles(String authorizationHeader, Long businessId) {
        try {
            VehicleBasicInfoResponse[] response = restClientBuilder
                    .baseUrl(fleetServiceUrl)
                    .build()
                    .get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/vehicles");
                        if (businessId != null) {
                            uriBuilder.queryParam("businessId", businessId);
                        }
                        return uriBuilder.build();
                    })
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve()
                    .body(VehicleBasicInfoResponse[].class);
            return response == null ? List.of() : Arrays.asList(response);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Could not load visible vehicles from Fleet Service", ex);
        }
    }
}

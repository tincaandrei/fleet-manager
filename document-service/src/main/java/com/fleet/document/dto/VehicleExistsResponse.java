package com.fleet.document.dto;

public record VehicleExistsResponse(
        Long id,
        boolean exists
) {
}

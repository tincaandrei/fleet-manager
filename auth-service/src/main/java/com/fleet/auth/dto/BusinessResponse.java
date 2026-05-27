package com.fleet.auth.dto;

import java.time.Instant;

public record BusinessResponse(
        Long id,
        String name,
        String registrationNumber,
        String contactEmail,
        String phone,
        String address,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}

package com.fleet.document.service;

import java.util.Set;

public record JwtPrincipal(
        String username,
        Long userId,
        Long businessId,
        Set<String> roles
) {
    @Override
    public String toString() {
        return username;
    }
}

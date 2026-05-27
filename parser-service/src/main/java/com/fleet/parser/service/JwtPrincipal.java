package com.fleet.parser.service;

import java.util.Set;

public record JwtPrincipal(
        String username,
        Long userId,
        Set<String> roles
) {
    @Override
    public String toString() {
        return username;
    }
}

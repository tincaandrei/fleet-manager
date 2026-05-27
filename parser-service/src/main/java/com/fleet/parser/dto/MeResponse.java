package com.fleet.parser.dto;

import java.util.Set;

public record MeResponse(
        String username,
        Long userId,
        Set<String> roles
) {
}

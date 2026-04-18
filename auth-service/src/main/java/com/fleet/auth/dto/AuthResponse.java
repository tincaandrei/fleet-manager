package com.fleet.auth.dto;

import com.fleet.auth.entity.Role;

public record AuthResponse(String token, String username, Role role) {
}

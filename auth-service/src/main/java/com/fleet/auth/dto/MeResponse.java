package com.fleet.auth.dto;

import com.fleet.auth.entity.Role;

public record MeResponse(Long userId, String username, String email, String phone, String address, Role role) {
}

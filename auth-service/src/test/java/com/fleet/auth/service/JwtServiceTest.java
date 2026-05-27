package com.fleet.auth.service;

import com.fleet.auth.config.JwtProperties;
import com.fleet.auth.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    @Test
    void generatesTokenWithUsernameAndSingleRoleClaim() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("01234567890123456789012345678901");
        properties.setExpirationMs(60_000);

        JwtService jwtService = new JwtService(properties);
        UserDetails userDetails = User.withUsername("alice")
                .password("ignored")
                .authorities(Role.toAuthorities(Role.SUPERADMIN))
                .build();

        String token = jwtService.generateToken(userDetails);

        assertEquals("alice", jwtService.extractUsername(token));
        assertEquals(Role.SUPERADMIN, jwtService.extractRole(token));
        assertTrue(jwtService.validateToken(token));
    }
}

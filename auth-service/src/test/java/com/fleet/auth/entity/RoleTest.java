package com.fleet.auth.entity;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoleTest {

    @Test
    void convertsBetweenAuthoritiesAndRoleNames() {
        assertEquals(Role.ADMIN, Role.fromValue("ROLE_ADMIN"));
        assertEquals(Role.USER, Role.fromAuthorities(List.of(new SimpleGrantedAuthority("ROLE_USER"))).iterator().next());
        assertEquals(Set.of(new SimpleGrantedAuthority("ROLE_ADMIN")), Role.toAuthorities(Role.ADMIN));
    }
}

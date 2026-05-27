package com.fleet.auth.entity;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoleTest {

    @Test
    void convertsBetweenAuthoritiesAndRoleNames() {
        assertEquals(Role.SUPERADMIN, Role.fromValue("ROLE_ADMIN"));
        assertEquals(Role.EMPLOYEE, Role.fromAuthorities(List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))).iterator().next());
        assertEquals(Set.of(new SimpleGrantedAuthority("ROLE_SUPERADMIN")), Role.toAuthorities(Role.SUPERADMIN));
    }
}

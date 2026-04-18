package com.fleet.auth.repository;

import com.fleet.auth.entity.Credential;
import com.fleet.auth.entity.Role;
import com.fleet.auth.entity.RoleEntity;
import com.fleet.auth.entity.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class NormalizedAuthRepositoryTest {

    @Autowired
    private RoleEntityRepository roleEntityRepository;

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private UserDataRepository userDataRepository;

    @BeforeEach
    void setUp() {
        if (roleEntityRepository.findByRoleName(Role.USER).isEmpty()) {
            roleEntityRepository.saveAndFlush(RoleEntity.builder().roleName(Role.USER).build());
        }
        if (roleEntityRepository.findByRoleName(Role.ADMIN).isEmpty()) {
            roleEntityRepository.saveAndFlush(RoleEntity.builder().roleName(Role.ADMIN).build());
        }
    }

    @Test
    void roleLookupByNameWorks() {
        RoleEntity roleEntity = roleEntityRepository.findByRoleName(Role.ADMIN).orElseThrow();

        assertEquals(Role.ADMIN, roleEntity.getRoleName());
    }

    @Test
    void credentialUsernameMustBeUnique() {
        RoleEntity userRole = roleEntityRepository.findByRoleName(Role.USER).orElseThrow();

        credentialRepository.saveAndFlush(Credential.builder()
                .username("alice")
                .passwordHash("hash")
                .role(userRole)
                .build());

        assertThrows(DataIntegrityViolationException.class, () ->
                credentialRepository.saveAndFlush(Credential.builder()
                        .username("alice")
                        .passwordHash("hash-2")
                        .role(userRole)
                        .build()));
    }

    @Test
    void userDataEmailAndCredentialLinkMustBeUnique() {
        RoleEntity userRole = roleEntityRepository.findByRoleName(Role.USER).orElseThrow();

        Credential firstCredential = credentialRepository.saveAndFlush(Credential.builder()
                .username("alice")
                .passwordHash("hash")
                .role(userRole)
                .build());

        userDataRepository.saveAndFlush(UserData.builder()
                .credential(firstCredential)
                .email("alice@example.com")
                .build());

        assertThrows(DataIntegrityViolationException.class, () ->
                userDataRepository.saveAndFlush(UserData.builder()
                        .credential(firstCredential)
                        .email("other@example.com")
                        .build()));
    }

    @Test
    void userDataEmailMustBeUnique() {
        RoleEntity userRole = roleEntityRepository.findByRoleName(Role.USER).orElseThrow();

        Credential firstCredential = credentialRepository.saveAndFlush(Credential.builder()
                .username("alice")
                .passwordHash("hash")
                .role(userRole)
                .build());
        Credential secondCredential = credentialRepository.saveAndFlush(Credential.builder()
                .username("bob")
                .passwordHash("hash-2")
                .role(userRole)
                .build());

        userDataRepository.saveAndFlush(UserData.builder()
                .credential(firstCredential)
                .email("alice@example.com")
                .build());

        assertThrows(DataIntegrityViolationException.class, () ->
                userDataRepository.saveAndFlush(UserData.builder()
                        .credential(secondCredential)
                        .email("alice@example.com")
                        .build()));
    }
}

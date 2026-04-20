package com.fleet.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleet.auth.config.BootstrapAdminProperties;
import com.fleet.auth.entity.Credential;
import com.fleet.auth.entity.Role;
import com.fleet.auth.entity.RoleEntity;
import com.fleet.auth.entity.UserData;
import com.fleet.auth.repository.CredentialRepository;
import com.fleet.auth.repository.RoleEntityRepository;
import com.fleet.auth.repository.UserDataRepository;
import com.fleet.auth.service.JwtService;
import com.fleet.auth.service.UserInfoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "security.jwt.secret=01234567890123456789012345678901",
        "spring.jpa.show-sql=false"
})
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private RoleEntityRepository roleEntityRepository;

    @Autowired
    private UserDataRepository userDataRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserInfoService userInfoService;

    @BeforeEach
    void setUp() {
        userDataRepository.deleteAll();
        credentialRepository.deleteAll();

        BootstrapAdminProperties properties = new BootstrapAdminProperties();
        properties.setUsername("admin");
        properties.setEmail("admin@example.com");
        properties.setPassword("admin123");
        userInfoService.ensureBootstrapAdmin(properties);
    }

    @Test
    void registerCreatesCredentialAndUserDataWithUserRole() throws Exception {
        String response = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "email": "alice@example.com",
                                  "password": "password123",
                                  "phone": "+40123456789",
                                  "address": "Main Street 1"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.phone").value("+40123456789"))
                .andExpect(jsonPath("$.address").value("Main Street 1"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(response);
        assertTrue(jsonNode.hasNonNull("userId"));

        Credential credential = credentialRepository.findByUsername("alice").orElseThrow();
        UserData userData = userDataRepository.findByEmail("alice@example.com").orElseThrow();
        assertEquals(userData.getCredential().getCredentialId(), credential.getCredentialId());
        assertEquals(Role.USER, credential.getRole().getRoleName());
        assertTrue(passwordEncoder.matches("password123", credential.getPasswordHash()));
    }

    @Test
    void loginUsesUsernameOnlyAndIssuesSingleRoleToken() throws Exception {
        createUser("alice", "alice@example.com", "password123", Role.USER, null, null);

        String token = login("alice", "password123");

        assertEquals("alice", jwtService.extractUsername(token));
        assertEquals(Role.USER, jwtService.extractRole(token));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void usersMeRequiresTokenAndReturnsLinkedProfileData() throws Exception {
        createUser("alice", "alice@example.com", "password123", Role.USER, "+40123456789", "Main Street 1");

        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));

        String token = login("alice", "password123");

        mockMvc.perform(get("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.phone").value("+40123456789"))
                .andExpect(jsonPath("$.address").value("Main Street 1"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void openApiEndpointsArePublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths['/auth/register']").exists())
                .andExpect(jsonPath("$.paths['/users/me']").exists());

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void bootstrapAdminIsLinkedToAdminRole() {
        Credential adminCredential = credentialRepository.findByUsername("admin").orElseThrow();
        UserData adminProfile = userDataRepository.findByEmail("admin@example.com").orElseThrow();

        assertEquals(Role.ADMIN, adminCredential.getRole().getRoleName());
        assertEquals(adminCredential.getCredentialId(), adminProfile.getCredential().getCredentialId());
    }

    @Test
    void adminEndpointReturnsForbiddenForUserToken() throws Exception {
        UserData targetUser = createUser("alice", "alice@example.com", "password123", Role.USER, null, null);
        String token = login("alice", "password123");

        mockMvc.perform(put("/admin/users/{id}/roles", targetUser.getUserId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void adminEndpointUpdatesCredentialRoleAndPersistsIt() throws Exception {
        UserData targetUser = createUser("alice", "alice@example.com", "password123", Role.USER, null, null);
        String adminToken = login("admin", "admin123");

        mockMvc.perform(put("/admin/users/{id}/roles", targetUser.getUserId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        Credential updatedCredential = credentialRepository.findByUsername("alice").orElseThrow();
        assertEquals(Role.ADMIN, updatedCredential.getRole().getRoleName());
    }

    @Test
    void roleChangesApplyAfterReloginNotToExistingTokens() throws Exception {
        UserData promotedUser = createUser("alice", "alice@example.com", "password123", Role.USER, null, null);
        UserData targetUser = createUser("charlie", "charlie@example.com", "password123", Role.USER, null, null);

        String oldUserToken = login("alice", "password123");
        String adminToken = login("admin", "admin123");

        mockMvc.perform(put("/admin/users/{id}/roles", promotedUser.getUserId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/admin/users/{id}/roles", targetUser.getUserId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + oldUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isForbidden());

        String newUserToken = login("alice", "password123");

        mockMvc.perform(put("/admin/users/{id}/roles", targetUser.getUserId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + newUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isOk());
    }

    private UserData createUser(String username, String email, String password, Role role, String phone, String address) {
        RoleEntity roleEntity = roleEntityRepository.findByRoleName(role).orElseThrow();

        Credential credential = credentialRepository.save(Credential.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .role(roleEntity)
                .build());

        UserData userData = userDataRepository.save(UserData.builder()
                .credential(credential)
                .email(email)
                .phone(phone)
                .address(address)
                .build());

        credential.setUserData(userData);
        return userData;
    }

    private String login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(response);
        assertTrue(jsonNode.hasNonNull("token"));
        assertNotNull(jsonNode.get("role"));
        return jsonNode.get("token").asText();
    }
}

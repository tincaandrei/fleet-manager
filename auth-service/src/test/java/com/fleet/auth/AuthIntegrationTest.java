package com.fleet.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleet.auth.config.BootstrapAdminProperties;
import com.fleet.auth.entity.Business;
import com.fleet.auth.entity.Credential;
import com.fleet.auth.entity.Role;
import com.fleet.auth.entity.RoleEntity;
import com.fleet.auth.entity.UserData;
import com.fleet.auth.repository.BusinessRepository;
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
import static org.junit.jupiter.api.Assertions.assertNull;
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
    private BusinessRepository businessRepository;

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
        businessRepository.deleteAll();

        BootstrapAdminProperties properties = new BootstrapAdminProperties();
        properties.setUsername("admin");
        properties.setEmail("admin@example.com");
        properties.setPassword("admin123");
        userInfoService.ensureBootstrapAdmin(properties);
    }

    @Test
    void registerCreatesUnassignedEmployeeAccount() throws Exception {
        String response = mockMvc.perform(post("/register")
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
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.businessId").doesNotExist())
                .andExpect(jsonPath("$.businessName").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(response);
        assertTrue(jsonNode.hasNonNull("userId"));

        Credential credential = credentialRepository.findByUsername("alice").orElseThrow();
        UserData userData = userDataRepository.findByEmail("alice@example.com").orElseThrow();
        assertEquals(userData.getCredential().getCredentialId(), credential.getCredentialId());
        assertEquals(Role.EMPLOYEE, credential.getRole().getRoleName());
        assertNull(userData.getBusiness());
        assertTrue(passwordEncoder.matches("password123", credential.getPasswordHash()));
    }

    @Test
    void registerRejectsDuplicateUsernameOrEmail() throws Exception {
        register("alice", "alice@example.com");

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "email": "alice2@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username is already taken"));

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice2",
                                  "email": "alice@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email is already taken"));
    }

    @Test
    void loginForUnassignedUserReturnsNullOrganizationFieldsAndProtectedOrgEndpointsAreForbidden() throws Exception {
        register("alice", "alice@example.com");

        String response = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.businessId").doesNotExist())
                .andExpect(jsonPath("$.businessName").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = objectMapper.readTree(response).get("token").asText();

        mockMvc.perform(post("/businesses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Acme Transport"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void superadminCanListAndAssignUnassignedUsers() throws Exception {
        Long userId = register("alice", "alice@example.com");
        Business business = createBusiness("Acme Transport", true);
        String adminToken = login("admin", "admin123");

        mockMvc.perform(get("/users/unassigned")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId))
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[0].businessId").doesNotExist());

        mockMvc.perform(put("/users/{id}/assignment", userId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "businessId": %d,
                                  "role": "BUSINESS_ADMIN"
                                }
                                """.formatted(business.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.role").value("BUSINESS_ADMIN"))
                .andExpect(jsonPath("$.businessId").value(business.getId()))
                .andExpect(jsonPath("$.businessName").value("Acme Transport"));

        UserData userData = userDataRepository.findById(userId).orElseThrow();
        assertEquals(business.getId(), userData.getBusiness().getId());
        assertEquals(Role.BUSINESS_ADMIN, userData.getCredential().getRole().getRoleName());
    }

    @Test
    void assignmentRequiresSuperadmin() throws Exception {
        Long userId = register("alice", "alice@example.com");
        Business business = createBusiness("Acme Transport", true);
        UserData employee = createUser("bob", "bob@example.com", "password123", Role.EMPLOYEE, business, null, null);
        String employeeToken = login(employee.getCredential().getUsername(), "password123");

        mockMvc.perform(put("/users/{id}/assignment", userId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "businessId": %d,
                                  "role": "EMPLOYEE"
                                }
                                """.formatted(business.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void assignmentRejectsInvalidRoleInactiveBusinessAndAlreadyAssignedUser() throws Exception {
        Long firstUserId = register("alice", "alice@example.com");
        Long secondUserId = register("charlie", "charlie@example.com");
        Business activeBusiness = createBusiness("Acme Transport", true);
        Business inactiveBusiness = createBusiness("Dormant Transport", false);
        String adminToken = login("admin", "admin123");

        mockMvc.perform(put("/users/{id}/assignment", firstUserId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "businessId": %d,
                                  "role": "SUPERADMIN"
                                }
                                """.formatted(activeBusiness.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Organization users cannot have SUPERADMIN role"));

        mockMvc.perform(put("/users/{id}/assignment", firstUserId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "businessId": %d,
                                  "role": "EMPLOYEE"
                                }
                                """.formatted(inactiveBusiness.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Organization is inactive"));

        mockMvc.perform(put("/users/{id}/assignment", firstUserId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "businessId": %d,
                                  "role": "EMPLOYEE"
                                }
                                """.formatted(activeBusiness.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(put("/users/{id}/assignment", firstUserId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "businessId": %d,
                                  "role": "EMPLOYEE"
                                }
                                """.formatted(activeBusiness.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User is already assigned to an organization"));

        mockMvc.perform(put("/users/{id}/assignment", secondUserId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "businessId": 999999,
                                  "role": "EMPLOYEE"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Organization not found: 999999"));
    }

    @Test
    void loginUsesUsernameOnlyAndIssuesSingleRoleToken() throws Exception {
        createUser("alice", "alice@example.com", "password123", Role.EMPLOYEE, null, "+40123456789", "Main Street 1");

        String token = login("alice", "password123");

        assertEquals("alice", jwtService.extractUsername(token));
        assertEquals(Role.EMPLOYEE, jwtService.extractRole(token));

        mockMvc.perform(post("/login")
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
        Business business = createBusiness("Acme Transport", true);
        createUser("alice", "alice@example.com", "password123", Role.EMPLOYEE, business, "+40123456789", "Main Street 1");

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
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.businessId").value(business.getId()))
                .andExpect(jsonPath("$.businessName").value("Acme Transport"));
    }

    @Test
    void openApiEndpointsArePublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths['/register']").exists())
                .andExpect(jsonPath("$.paths['/login']").exists())
                .andExpect(jsonPath("$.paths['/users/me']").exists());

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void bootstrapAdminIsLinkedToAdminRole() {
        Credential adminCredential = credentialRepository.findByUsername("admin").orElseThrow();
        UserData adminProfile = userDataRepository.findByEmail("admin@example.com").orElseThrow();

        assertEquals(Role.SUPERADMIN, adminCredential.getRole().getRoleName());
        assertEquals(adminCredential.getCredentialId(), adminProfile.getCredential().getCredentialId());
        assertNull(adminProfile.getBusiness());
    }

    private Long register(String username, String email) throws Exception {
        String response = mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "email": "%s",
                                  "password": "password123"
                                }
                                """.formatted(username, email)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("userId").asLong();
    }

    private UserData createUser(String username, String email, String password, Role role, Business business, String phone, String address) {
        RoleEntity roleEntity = roleEntityRepository.findByRoleName(role)
                .orElseGet(() -> roleEntityRepository.save(RoleEntity.builder().roleName(role).build()));

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
                .business(business)
                .build());

        credential.setUserData(userData);
        return userData;
    }

    private Business createBusiness(String name, boolean active) {
        return businessRepository.save(Business.builder()
                .name(name)
                .active(active)
                .build());
    }

    private String login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/login")
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

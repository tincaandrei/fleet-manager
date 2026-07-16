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
import com.fleet.auth.repository.InvitationTokenRepository;
import com.fleet.auth.repository.RoleEntityRepository;
import com.fleet.auth.repository.UserDataRepository;
import com.fleet.auth.service.JwtService;
import com.fleet.auth.service.MailService;
import com.fleet.auth.service.UserInfoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    private InvitationTokenRepository invitationTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserInfoService userInfoService;

    @MockBean
    private MailService mailService;

    @BeforeEach
    void setUp() {
        invitationTokenRepository.deleteAll();
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
    void publicRegistrationIsDisabled() throws Exception {
        mockMvc.perform(post("/register")
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
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void adminCanCreateInvitedUserAndSendEmail() throws Exception {
        String adminToken = login("admin", "admin123");

        mockMvc.perform(post("/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "invitee@example.com",
                                  "firstName": "Andrei",
                                  "lastName": "Popescu",
                                  "roles": ["USER"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("invitee@example.com"))
                .andExpect(jsonPath("$.firstName").value("Andrei"))
                .andExpect(jsonPath("$.lastName").value("Popescu"))
                .andExpect(jsonPath("$.roles[0]").value("EMPLOYEE"))
                .andExpect(jsonPath("$.status").value("INVITED"))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.passwordChangeRequired").value(true));

        UserData userData = userDataRepository.findByEmail("invitee@example.com").orElseThrow();
        assertEquals(Role.EMPLOYEE, userData.getCredential().getRole().getRoleName().canonical());
        assertEquals(com.fleet.auth.entity.UserStatus.INVITED, userData.getCredential().getStatus());
        assertNotNull(userData.getCredential().getPasswordHash());
        verify(mailService).sendUserInvitationEmail(eq("invitee@example.com"), startsWith("http://localhost:5173/accept-invite?token="), eq(24L));
    }

    @Test
    void inviteAcceptanceActivatesUserAndAllowsLogin() throws Exception {
        String adminToken = login("admin", "admin123");

        mockMvc.perform(post("/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "invitee@example.com",
                                  "firstName": "Andrei",
                                  "lastName": "Popescu",
                                  "roles": ["USER"]
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "invitee@example.com",
                                  "password": "anything"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("INVITATION_NOT_ACCEPTED"));

        org.mockito.ArgumentCaptor<String> linkCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(mailService).sendUserInvitationEmail(eq("invitee@example.com"), linkCaptor.capture(), eq(24L));
        String rawToken = linkCaptor.getValue().substring(linkCaptor.getValue().indexOf("token=") + 6);
        assertTrue(invitationTokenRepository.findAll().stream()
                .noneMatch(token -> token.getTokenHash().equals(rawToken)));

        mockMvc.perform(get("/invitations/validate")
                        .param("token", rawToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.email").value("i***@example.com"));

        mockMvc.perform(post("/accept-invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "%s",
                                  "newPassword": "StrongPassword123!"
                                }
                                """.formatted(rawToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password set successfully. You can now log in."));

        UserData userData = userDataRepository.findByEmail("invitee@example.com").orElseThrow();
        assertEquals(com.fleet.auth.entity.UserStatus.ACTIVE, userData.getCredential().getStatus());
        assertTrue(Boolean.TRUE.equals(userData.getCredential().getEnabled()));
        assertTrue(Boolean.FALSE.equals(userData.getCredential().getPasswordChangeRequired()));
        assertTrue(passwordEncoder.matches("StrongPassword123!", userData.getCredential().getPasswordHash()));
        assertNotNull(invitationTokenRepository.findAll().get(0).getUsedAt());

        mockMvc.perform(post("/accept-invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "%s",
                                  "newPassword": "AnotherPassword123!"
                                }
                                """.formatted(rawToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("INVITATION_TOKEN_USED"));

        mockMvc.perform(post("/accept-invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "not-a-real-token",
                                  "newPassword": "AnotherPassword123!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("INVITATION_TOKEN_INVALID"));

        login("invitee@example.com", "StrongPassword123!");
    }

    @Test
    void inviteAcceptanceRejectsPasswordWithoutSpecialCharacter() throws Exception {
        String adminToken = login("admin", "admin123");

        mockMvc.perform(post("/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "invitee@example.com",
                                  "firstName": "Andrei",
                                  "lastName": "Popescu",
                                  "roles": ["USER"]
                                }
                                """))
                .andExpect(status().isCreated());

        org.mockito.ArgumentCaptor<String> linkCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(mailService).sendUserInvitationEmail(eq("invitee@example.com"), linkCaptor.capture(), eq(24L));
        String rawToken = linkCaptor.getValue().substring(linkCaptor.getValue().indexOf("token=") + 6);

        mockMvc.perform(post("/accept-invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "%s",
                                  "newPassword": "StrongPassword123"
                                }
                                """.formatted(rawToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("PASSWORD_TOO_WEAK"));

        UserData userData = userDataRepository.findByEmail("invitee@example.com").orElseThrow();
        assertEquals(com.fleet.auth.entity.UserStatus.INVITED, userData.getCredential().getStatus());
        assertNull(invitationTokenRepository.findAll().get(0).getUsedAt());
    }

    @Test
    void normalUserCannotCreateInvitedUserAndDuplicateEmailIsRejected() throws Exception {
        createUser("employee", "employee@example.com", "password123", Role.EMPLOYEE, null, null, null);
        String employeeToken = login("employee", "password123");

        mockMvc.perform(post("/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "invitee@example.com",
                                  "firstName": "Andrei",
                                  "lastName": "Popescu",
                                  "roles": ["USER"]
                                }
                                """))
                .andExpect(status().isForbidden());

        String adminToken = login("admin", "admin123");
        createUser("invitee@example.com", "invitee@example.com", "password123", Role.EMPLOYEE, null, null, null);
        mockMvc.perform(post("/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "invitee@example.com",
                                  "firstName": "Andrei",
                                  "lastName": "Popescu",
                                  "roles": ["USER"]
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void resendInviteOnlyWorksForInvitedUsersAndDisabledUserCannotLogin() throws Exception {
        String adminToken = login("admin", "admin123");

        String response = mockMvc.perform(post("/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "invitee@example.com",
                                  "firstName": "Andrei",
                                  "lastName": "Popescu",
                                  "roles": ["USER"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long invitedUserId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(post("/admin/users/{id}/resend-invite", invitedUserId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());
        verify(mailService, times(2)).sendUserInvitationEmail(eq("invitee@example.com"), startsWith("http://localhost:5173/accept-invite?token="), eq(24L));

        UserData activeUser = createUser("active", "active@example.com", "password123", Role.EMPLOYEE, null, null, null);
        mockMvc.perform(post("/admin/users/{id}/resend-invite", activeUser.getUserId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("INVITATION_NOT_ACCEPTED"));

        mockMvc.perform(patch("/admin/users/{id}/status", activeUser.getUserId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "DISABLED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "active",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("ACCOUNT_DISABLED"));
    }

    @Test
    void loginForUnassignedUserReturnsNullOrganizationFieldsAndProtectedOrgEndpointsAreForbidden() throws Exception {
        createUser("alice", "alice@example.com", "password123", Role.EMPLOYEE, null, null, null);

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
        Long userId = createUser("alice", "alice@example.com", "password123", Role.EMPLOYEE, null, null, null).getUserId();
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
        Long userId = createUser("alice", "alice@example.com", "password123", Role.EMPLOYEE, null, null, null).getUserId();
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
        Long firstUserId = createUser("alice", "alice@example.com", "password123", Role.EMPLOYEE, null, null, null).getUserId();
        Long secondUserId = createUser("charlie", "charlie@example.com", "password123", Role.EMPLOYEE, null, null, null).getUserId();
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
    void loginUsesEmailAndIssuesSingleRoleToken() throws Exception {
        createUser("alice", "alice@example.com", "password123", Role.EMPLOYEE, null, "+40123456789", "Main Street 1");

        String token = login("alice@example.com", "password123");

        assertEquals("alice@example.com", jwtService.extractUsername(token));
        assertEquals(Role.EMPLOYEE, jwtService.extractRole(token));
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
    void lookupUsersReturnsOnlyVisibleUsers() throws Exception {
        Business business = createBusiness("Acme Transport", true);
        UserData admin = createUser("manager", "manager@example.com", "password123", Role.BUSINESS_ADMIN, business, null, null);
        UserData employee = createUser("driver", "driver@example.com", "password123", Role.EMPLOYEE, business, null, null);
        UserData outsider = createUser("outsider", "outsider@example.com", "password123", Role.EMPLOYEE, null, null, null);

        String adminToken = login("manager", "password123");
        mockMvc.perform(get("/users/lookup")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("ids", employee.getUserId().toString(), outsider.getUserId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(employee.getUserId()))
                .andExpect(jsonPath("$[0].username").value("driver"))
                .andExpect(jsonPath("$[0].email").value("driver@example.com"))
                .andExpect(jsonPath("$[0].businessId").value(business.getId()))
                .andExpect(jsonPath("$[0].role").value("EMPLOYEE"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].enabled").value(true))
                .andExpect(jsonPath("$[1]").doesNotExist());

        String employeeToken = login("driver", "password123");
        mockMvc.perform(get("/users/lookup")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
                        .param("ids", admin.getUserId().toString(), employee.getUserId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(employee.getUserId()))
                .andExpect(jsonPath("$[1]").doesNotExist());
    }

    @Test
    void internalBusinessAdminLookupReturnsActiveAdminsForSameBusiness() throws Exception {
        Business business = createBusiness("Acme Transport", true);
        Business otherBusiness = createBusiness("Other Transport", true);
        UserData admin = createUser("manager", "manager@example.com", "password123", Role.BUSINESS_ADMIN, business, null, null);
        createUser("driver", "driver@example.com", "password123", Role.EMPLOYEE, business, null, null);
        UserData disabledAdmin = createUser("disabled-admin", "disabled-admin@example.com", "password123", Role.BUSINESS_ADMIN, business, null, null);
        disabledAdmin.getCredential().setStatus(com.fleet.auth.entity.UserStatus.DISABLED);
        disabledAdmin.getCredential().setEnabled(false);
        credentialRepository.save(disabledAdmin.getCredential());
        createUser("outside-admin", "outside-admin@example.com", "password123", Role.BUSINESS_ADMIN, otherBusiness, null, null);

        String employeeToken = login("driver", "password123");
        mockMvc.perform(get("/internal/businesses/{businessId}/admins", business.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(admin.getUserId()))
                .andExpect(jsonPath("$[0].role").value("BUSINESS_ADMIN"))
                .andExpect(jsonPath("$[1]").doesNotExist());

        mockMvc.perform(get("/internal/businesses/{businessId}/admins", otherBusiness.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
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

    @Test
    void superadminCanSendResetLinkAcrossOrganizationsAndUserCanSetNewPassword() throws Exception {
        Business business = createBusiness("Acme Transport", true);
        UserData employee = createUser("driver", "driver@example.com", "password123", Role.EMPLOYEE, business, null, null);
        String adminToken = login("admin", "admin123");

        mockMvc.perform(post("/admin/users/{id}/password-reset-link", employee.getUserId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("driver@example.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        org.mockito.ArgumentCaptor<String> firstLinkCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(mailService).sendPasswordResetEmail(eq("driver@example.com"), firstLinkCaptor.capture(), eq(24L));
        String firstToken = firstLinkCaptor.getValue().substring(firstLinkCaptor.getValue().indexOf("token=") + 6);

        // A second reset link invalidates the first unused token.
        mockMvc.perform(post("/admin/users/{id}/password-reset-link", employee.getUserId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<String> secondLinkCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(mailService, times(2)).sendPasswordResetEmail(eq("driver@example.com"), secondLinkCaptor.capture(), eq(24L));
        String secondToken = secondLinkCaptor.getValue().substring(secondLinkCaptor.getValue().indexOf("token=") + 6);

        mockMvc.perform(get("/invitations/validate")
                        .param("token", firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("INVITATION_TOKEN_USED"));

        mockMvc.perform(post("/accept-invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "%s",
                                  "newPassword": "BrandNewPassword1!"
                                }
                                """.formatted(secondToken)))
                .andExpect(status().isOk());

        UserData refreshed = userDataRepository.findByEmail("driver@example.com").orElseThrow();
        assertEquals(com.fleet.auth.entity.UserStatus.ACTIVE, refreshed.getCredential().getStatus());
        assertTrue(passwordEncoder.matches("BrandNewPassword1!", refreshed.getCredential().getPasswordHash()));

        login("driver@example.com", "BrandNewPassword1!");
    }

    @Test
    void businessAdminCanResetOwnOrgUsersButNotOtherOrganizations() throws Exception {
        Business ownBusiness = createBusiness("Acme Transport", true);
        Business otherBusiness = createBusiness("Globex Logistics", true);
        createUser("manager", "manager@example.com", "password123", Role.BUSINESS_ADMIN, ownBusiness, null, null);
        UserData ownEmployee = createUser("driver", "driver@example.com", "password123", Role.EMPLOYEE, ownBusiness, null, null);
        UserData otherEmployee = createUser("outsider", "outsider@example.com", "password123", Role.EMPLOYEE, otherBusiness, null, null);
        String managerToken = login("manager", "password123");

        mockMvc.perform(post("/admin/users/{id}/password-reset-link", ownEmployee.getUserId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
                .andExpect(status().isOk());
        verify(mailService).sendPasswordResetEmail(eq("driver@example.com"), startsWith("http://localhost:5173/accept-invite?token="), eq(24L));

        mockMvc.perform(post("/admin/users/{id}/password-reset-link", otherEmployee.getUserId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void resetLinkIsRejectedForSuperadminTargetsAndInvitedUsers() throws Exception {
        String adminToken = login("admin", "admin123");
        UserData otherSuperadmin = createUser("root2", "root2@example.com", "password123", Role.SUPERADMIN, null, null, null);

        mockMvc.perform(post("/admin/users/{id}/password-reset-link", otherSuperadmin.getUserId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));

        String response = mockMvc.perform(post("/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "invitee@example.com",
                                  "firstName": "Andrei",
                                  "lastName": "Popescu",
                                  "roles": ["USER"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long invitedUserId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(post("/admin/users/{id}/password-reset-link", invitedUserId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("INVITATION_NOT_ACCEPTED"));
    }

    @Test
    void disabledUserStaysDisabledAfterSettingPasswordThroughResetLink() throws Exception {
        Business business = createBusiness("Acme Transport", true);
        UserData employee = createUser("driver", "driver@example.com", "password123", Role.EMPLOYEE, business, null, null);
        Credential credential = employee.getCredential();
        credential.setStatus(com.fleet.auth.entity.UserStatus.DISABLED);
        credential.setEnabled(false);
        credentialRepository.save(credential);

        String adminToken = login("admin", "admin123");
        mockMvc.perform(post("/admin/users/{id}/password-reset-link", employee.getUserId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));

        org.mockito.ArgumentCaptor<String> linkCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(mailService).sendPasswordResetEmail(eq("driver@example.com"), linkCaptor.capture(), eq(24L));
        String rawToken = linkCaptor.getValue().substring(linkCaptor.getValue().indexOf("token=") + 6);

        mockMvc.perform(post("/accept-invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "%s",
                                  "newPassword": "BrandNewPassword1!"
                                }
                                """.formatted(rawToken)))
                .andExpect(status().isOk());

        UserData refreshed = userDataRepository.findByEmail("driver@example.com").orElseThrow();
        assertEquals(com.fleet.auth.entity.UserStatus.DISABLED, refreshed.getCredential().getStatus());
        assertTrue(Boolean.FALSE.equals(refreshed.getCredential().getEnabled()));
        assertTrue(passwordEncoder.matches("BrandNewPassword1!", refreshed.getCredential().getPasswordHash()));

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "driver@example.com",
                                  "password": "BrandNewPassword1!"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("ACCOUNT_DISABLED"));
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
                .email(email)
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
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(response);
        assertTrue(jsonNode.hasNonNull("token"));
        assertTrue(jsonNode.hasNonNull("email"));
        assertNotNull(jsonNode.get("role"));
        return jsonNode.get("token").asText();
    }
}

package com.fleet.auth.controller;

import com.fleet.auth.dto.AuthResponse;
import com.fleet.auth.dto.AcceptInviteRequest;
import com.fleet.auth.dto.AdminCreateUserRequest;
import com.fleet.auth.dto.AdminUserResponse;
import com.fleet.auth.dto.AssignBusinessUserRequest;
import com.fleet.auth.dto.BusinessRequest;
import com.fleet.auth.dto.BusinessResponse;
import com.fleet.auth.dto.CreateBusinessUserRequest;
import com.fleet.auth.dto.ErrorResponse;
import com.fleet.auth.dto.InviteValidationResponse;
import com.fleet.auth.dto.LoginRequest;
import com.fleet.auth.dto.MeResponse;
import com.fleet.auth.dto.MessageResponse;
import com.fleet.auth.dto.RegisterRequest;
import com.fleet.auth.dto.UpdateRoleRequest;
import com.fleet.auth.dto.UpdateUserRequest;
import com.fleet.auth.dto.UpdateUserStatusRequest;
import com.fleet.auth.dto.UserLookupResponse;
import com.fleet.auth.entity.Role;
import com.fleet.auth.service.JwtService;
import com.fleet.auth.service.CredentialDetails;
import com.fleet.auth.service.UserInfoService;
import com.fleet.auth.service.UserInvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication and user management endpoints.")
public class UserController {

    private final UserInfoService userInfoService;
    private final UserInvitationService userInvitationService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    @Operation(summary = "Register a user", description = "Creates a new account with USER role.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered", content = @Content(schema = @Schema(implementation = MeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Username or email already exists", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MeResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        throw new org.springframework.security.access.AccessDeniedException("Public registration is disabled");
    }

    @Hidden
    @PostMapping("/auth/register")
    public ResponseEntity<MeResponse> legacyRegister(@Valid @RequestBody RegisterRequest registerRequest) {
        return register(registerRequest);
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticates with username/password and returns a JWT token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        String loginEmail = loginRequest.loginEmail();
        userInfoService.assertCanLogin(loginEmail);
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginEmail, loginRequest.password())
        );
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);
        Role role = userDetails instanceof CredentialDetails credentialDetails
                ? credentialDetails.getRole()
                : Role.fromAuthorities(userDetails.getAuthorities()).stream().findFirst().orElseThrow();
        Long userId = userDetails instanceof CredentialDetails credentialDetails ? credentialDetails.getUserId() : null;
        Long businessId = userDetails instanceof CredentialDetails credentialDetails ? credentialDetails.getBusinessId() : null;
        String businessName = userDetails instanceof CredentialDetails credentialDetails ? credentialDetails.getBusinessName() : null;
        String username = userDetails instanceof CredentialDetails credentialDetails ? credentialDetails.getDisplayUsername() : userDetails.getUsername();
        String email = userDetails instanceof CredentialDetails credentialDetails ? credentialDetails.getEmail() : userDetails.getUsername();
        return ResponseEntity.ok(new AuthResponse(token, username, email, role, userId, businessId, businessName));
    }

    @Hidden
    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> legacyLogin(@Valid @RequestBody LoginRequest loginRequest) {
        return login(loginRequest);
    }

    @PostMapping("/accept-invite")
    @Operation(summary = "Accept invitation", description = "Sets a password for an invited user and activates the account.")
    public ResponseEntity<MessageResponse> acceptInvite(@Valid @RequestBody AcceptInviteRequest request) {
        return ResponseEntity.ok(userInvitationService.acceptInvite(request));
    }

    @GetMapping("/invitations/validate")
    @Operation(summary = "Validate invitation", description = "Checks whether an invitation token can still be used.")
    public ResponseEntity<InviteValidationResponse> validateInvite(@RequestParam("token") String token) {
        return ResponseEntity.ok(userInvitationService.validateInvite(token));
    }

    @GetMapping("/users/me")
    @Operation(summary = "Current user profile", description = "Returns profile data for the authenticated user.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile returned", content = @Content(schema = @Schema(implementation = MeResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User profile not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MeResponse> me(Authentication authentication) {
        return ResponseEntity.ok(userInfoService.getCurrentUser(authentication.getName()));
    }

    @PutMapping("/users/me")
    @Operation(summary = "Update current user profile", description = "Updates email, phone, address, and optionally password for the authenticated user.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated", content = @Content(schema = @Schema(implementation = MeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payload", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email already exists", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MeResponse> updateMe(@Valid @RequestBody UpdateUserRequest request,
                                               Authentication authentication) {
        return ResponseEntity.ok(userInfoService.updateCurrentUser(request, authentication));
    }

    @PostMapping(value = "/users/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload current user profile image", description = "Stores or replaces the authenticated user's profile image.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile image saved", content = @Content(schema = @Schema(implementation = MeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid image", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MeResponse> uploadProfileImage(
            @Parameter(description = "JPG, PNG, or WebP profile image, max 5 MB.", required = true)
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        return ResponseEntity.ok(userInfoService.uploadCurrentUserProfileImage(file, authentication));
    }

    @GetMapping("/users/me/profile-image")
    @Operation(summary = "Download current user profile image", description = "Returns the authenticated user's profile image.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile image returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Profile image not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Resource> downloadProfileImage(Authentication authentication) {
        UserInfoService.ProfileImageResource image = userInfoService.loadCurrentUserProfileImage(authentication);
        return profileImageResponse(image);
    }

    @GetMapping("/users/{id}/profile-image")
    @Operation(summary = "Download user profile image", description = "Returns a user's profile image when the requester can view that user.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Resource> downloadUserProfileImage(
            @Parameter(description = "User id.", example = "1") @PathVariable Long id,
            Authentication authentication
    ) {
        UserInfoService.ProfileImageResource image = userInfoService.loadUserProfileImage(id, authentication);
        return profileImageResponse(image);
    }

    private ResponseEntity<Resource> profileImageResponse(UserInfoService.ProfileImageResource image) {
        MediaType mediaType = image.contentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(image.contentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(image.originalFileName() == null ? "profile-image" : image.originalFileName())
                        .build()
                        .toString())
                .body(image.resource());
    }

    @DeleteMapping("/users/me/profile-image")
    @Operation(summary = "Delete current user profile image", description = "Removes the authenticated user's profile image.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile image removed", content = @Content(schema = @Schema(implementation = MeResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MeResponse> deleteProfileImage(Authentication authentication) {
        return ResponseEntity.ok(userInfoService.deleteCurrentUserProfileImage(authentication));
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "Update user profile", description = "Updates any user profile. Requires SUPERADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<MeResponse> updateUser(
            @Parameter(description = "User id to update.", example = "1") @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(userInfoService.updateUser(id, request, authentication));
    }

    @PostMapping("/businesses")
    @Operation(summary = "Create business", description = "Requires SUPERADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<BusinessResponse> createBusiness(@Valid @RequestBody BusinessRequest request,
                                                           Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userInfoService.createBusiness(request, authentication));
    }

    @GetMapping("/businesses")
    @Operation(summary = "List businesses", description = "Requires SUPERADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<BusinessResponse>> listBusinesses(Authentication authentication) {
        return ResponseEntity.ok(userInfoService.listBusinesses(authentication));
    }

    @GetMapping("/businesses/{businessId}")
    @Operation(summary = "Get business", description = "Requires SUPERADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<BusinessResponse> getBusiness(@PathVariable Long businessId,
                                                        Authentication authentication) {
        return ResponseEntity.ok(userInfoService.getBusiness(businessId, authentication));
    }

    @PutMapping("/businesses/{businessId}")
    @Operation(summary = "Update business", description = "Requires SUPERADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<BusinessResponse> updateBusiness(@PathVariable Long businessId,
                                                           @Valid @RequestBody BusinessRequest request,
                                                           Authentication authentication) {
        return ResponseEntity.ok(userInfoService.updateBusiness(businessId, request, authentication));
    }

    @PostMapping("/businesses/{businessId}/users")
    @Operation(summary = "Create business user", description = "Requires SUPERADMIN or same-business BUSINESS_ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<MeResponse> createBusinessUser(@PathVariable Long businessId,
                                                         @Valid @RequestBody CreateBusinessUserRequest request,
                                                         Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userInfoService.createBusinessUser(businessId, request, authentication));
    }

    @GetMapping("/businesses/{businessId}/users")
    @Operation(summary = "List business users", description = "Requires SUPERADMIN or same-business BUSINESS_ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<MeResponse>> listBusinessUsers(@PathVariable Long businessId,
                                                              Authentication authentication) {
        return ResponseEntity.ok(userInfoService.listBusinessUsers(businessId, authentication));
    }

    @GetMapping("/users/unassigned")
    @Operation(summary = "List unassigned users", description = "Requires SUPERADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<MeResponse>> listUnassignedUsers(Authentication authentication) {
        return ResponseEntity.ok(userInfoService.listUnassignedUsers(authentication));
    }

    @PostMapping("/admin/users")
    @Operation(summary = "Create invited user", description = "Creates an invited user and sends a setup email.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<AdminUserResponse> createInvitedUser(
            @Valid @RequestBody AdminCreateUserRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userInfoService.createInvitedUser(request, authentication));
    }

    @PostMapping("/admin/users/{id}/resend-invite")
    @Operation(summary = "Resend invitation", description = "Sends a fresh invitation link for an invited user.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<AdminUserResponse> resendInvite(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(userInfoService.resendInvite(id, authentication));
    }

    @PatchMapping("/admin/users/{id}/status")
    @Operation(summary = "Update user status", description = "Enables or disables a user account.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<AdminUserResponse> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(userInfoService.updateUserStatus(id, request, authentication));
    }

    @GetMapping("/users/lookup")
    @Operation(summary = "Lookup users", description = "Returns minimal user display data for visible user ids.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<UserLookupResponse>> lookupUsers(
            @Parameter(description = "User ids to lookup.", example = "1,2,3")
            @RequestParam(name = "ids") List<Long> ids,
            Authentication authentication
    ) {
        return ResponseEntity.ok(userInfoService.lookupUsers(ids, authentication));
    }

    @PutMapping("/users/{id}/assignment")
    @Operation(summary = "Assign unassigned user to organization", description = "Requires SUPERADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<MeResponse> assignUser(
            @Parameter(description = "User id to assign.", example = "1") @PathVariable Long id,
            @Valid @RequestBody AssignBusinessUserRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(userInfoService.assignUnassignedUser(id, request, authentication));
    }

    @PutMapping("/businesses/{businessId}/users/{id}")
    @Operation(summary = "Update business user profile", description = "Updates a business user's email, phone, address, and optionally password. Requires SUPERADMIN or same-business BUSINESS_ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<MeResponse> updateBusinessUser(
            @Parameter(description = "Business id.", example = "1") @PathVariable Long businessId,
            @Parameter(description = "User id to update.", example = "1") @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(userInfoService.updateBusinessUser(businessId, id, request, authentication));
    }

    @PutMapping("/businesses/{businessId}/users/{id}/role")
    @Operation(summary = "Update business user role", description = "Requires SUPERADMIN or same-business BUSINESS_ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role updated", content = @Content(schema = @Schema(implementation = MeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid role or payload", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Admin role required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Target user not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MeResponse> updateRoles(
            @Parameter(description = "Business id.", example = "1") @PathVariable Long businessId,
            @Parameter(description = "User id to update.", example = "1") @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(userInfoService.updateRole(businessId, id, request.role(), authentication));
    }
}

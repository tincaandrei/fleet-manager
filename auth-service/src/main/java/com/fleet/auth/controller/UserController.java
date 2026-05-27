package com.fleet.auth.controller;

import com.fleet.auth.dto.AuthResponse;
import com.fleet.auth.dto.BusinessRequest;
import com.fleet.auth.dto.BusinessResponse;
import com.fleet.auth.dto.CreateBusinessUserRequest;
import com.fleet.auth.dto.ErrorResponse;
import com.fleet.auth.dto.LoginRequest;
import com.fleet.auth.dto.MeResponse;
import com.fleet.auth.dto.RegisterRequest;
import com.fleet.auth.dto.UpdateRoleRequest;
import com.fleet.auth.dto.UpdateUserRequest;
import com.fleet.auth.entity.Role;
import com.fleet.auth.service.JwtService;
import com.fleet.auth.service.CredentialDetails;
import com.fleet.auth.service.UserInfoService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication and user management endpoints.")
public class UserController {

    private final UserInfoService userInfoService;
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
        MeResponse response = userInfoService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password())
        );
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);
        Role role = userDetails instanceof CredentialDetails credentialDetails
                ? credentialDetails.getRole()
                : Role.fromAuthorities(userDetails.getAuthorities()).stream().findFirst().orElseThrow();
        Long userId = userDetails instanceof CredentialDetails credentialDetails ? credentialDetails.getUserId() : null;
        Long businessId = userDetails instanceof CredentialDetails credentialDetails ? credentialDetails.getBusinessId() : null;
        String businessName = userDetails instanceof CredentialDetails credentialDetails ? credentialDetails.getBusinessName() : null;
        return ResponseEntity.ok(new AuthResponse(token, userDetails.getUsername(), role, userId, businessId, businessName));
    }

    @Hidden
    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> legacyLogin(@Valid @RequestBody LoginRequest loginRequest) {
        return login(loginRequest);
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

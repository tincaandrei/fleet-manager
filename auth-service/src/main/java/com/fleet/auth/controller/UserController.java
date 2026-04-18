package com.fleet.auth.controller;

import com.fleet.auth.dto.AuthResponse;
import com.fleet.auth.dto.LoginRequest;
import com.fleet.auth.dto.MeResponse;
import com.fleet.auth.dto.RegisterRequest;
import com.fleet.auth.dto.UpdateRoleRequest;
import com.fleet.auth.entity.Role;
import com.fleet.auth.service.JwtService;
import com.fleet.auth.service.CredentialDetails;
import com.fleet.auth.service.UserInfoService;
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

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserInfoService userInfoService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/auth/register")
    public ResponseEntity<MeResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        MeResponse response = userInfoService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password())
        );
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);
        Role role = userDetails instanceof CredentialDetails credentialDetails
                ? credentialDetails.getRole()
                : Role.fromAuthorities(userDetails.getAuthorities()).stream().findFirst().orElseThrow();
        return ResponseEntity.ok(new AuthResponse(token, userDetails.getUsername(), role));
    }

    @GetMapping("/users/me")
    public ResponseEntity<MeResponse> me(Authentication authentication) {
        return ResponseEntity.ok(userInfoService.getCurrentUser(authentication.getName()));
    }

    @PutMapping("/admin/users/{id}/roles")
    public ResponseEntity<MeResponse> updateRoles(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(userInfoService.updateRole(id, request.role()));
    }
}

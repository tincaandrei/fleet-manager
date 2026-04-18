package com.fleet.auth.service;

import com.fleet.auth.config.BootstrapAdminProperties;
import com.fleet.auth.entity.Credential;
import com.fleet.auth.dto.MeResponse;
import com.fleet.auth.dto.RegisterRequest;
import com.fleet.auth.entity.Role;
import com.fleet.auth.entity.RoleEntity;
import com.fleet.auth.entity.UserData;
import com.fleet.auth.exception.DuplicateUserException;
import com.fleet.auth.exception.ResourceNotFoundException;
import com.fleet.auth.repository.CredentialRepository;
import com.fleet.auth.repository.RoleEntityRepository;
import com.fleet.auth.repository.UserDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserInfoService implements UserDetailsService {

    private final CredentialRepository credentialRepository;
    private final RoleEntityRepository roleEntityRepository;
    private final UserDataRepository userDataRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Credential credential = credentialRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return new CredentialDetails(credential);
    }

    @Transactional
    public MeResponse register(RegisterRequest registerRequest) {
        validateUniqueUser(registerRequest.username(), registerRequest.email());

        Credential credential = Credential.builder()
                .username(registerRequest.username())
                .passwordHash(passwordEncoder.encode(registerRequest.password()))
                .role(getOrCreateRole(Role.USER))
                .build();
        Credential savedCredential = credentialRepository.save(credential);

        UserData userData = UserData.builder()
                .credential(savedCredential)
                .email(registerRequest.email())
                .phone(registerRequest.phone())
                .address(registerRequest.address())
                .build();

        UserData savedUserData = userDataRepository.save(userData);
        savedCredential.setUserData(savedUserData);
        return toMeResponse(savedUserData);
    }

    @Transactional
    public MeResponse updateRole(Long userId, Role role) {
        UserData userData = userDataRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        userData.getCredential().setRole(getOrCreateRole(role));
        credentialRepository.save(userData.getCredential());
        return toMeResponse(userData);
    }

    @Transactional(readOnly = true)
    public MeResponse getCurrentUser(String username) {
        UserData userData = userDataRepository.findByCredentialUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return toMeResponse(userData);
    }

    @Transactional
    public void ensureBootstrapAdmin(BootstrapAdminProperties bootstrapAdminProperties) {
        boolean hasUsername = StringUtils.hasText(bootstrapAdminProperties.getUsername());
        boolean hasEmail = StringUtils.hasText(bootstrapAdminProperties.getEmail());
        boolean hasPassword = StringUtils.hasText(bootstrapAdminProperties.getPassword());

        if (!hasUsername && !hasEmail && !hasPassword) {
            return;
        }
        if (!hasUsername || !hasEmail || !hasPassword) {
            throw new IllegalStateException("Bootstrap admin username, email, and password must all be provided");
        }

        Optional<Credential> byUsername = credentialRepository.findByUsername(bootstrapAdminProperties.getUsername());
        Optional<UserData> byEmail = userDataRepository.findByEmail(bootstrapAdminProperties.getEmail());

        if (byUsername.isPresent() && byEmail.isPresent()
                && !byUsername.get().getCredentialId().equals(byEmail.get().getCredential().getCredentialId())) {
            throw new IllegalStateException("Bootstrap admin username and email refer to different users");
        }

        Credential credential = byUsername.orElseGet(() -> byEmail.map(UserData::getCredential).orElseGet(Credential::new));
        credential.setUsername(bootstrapAdminProperties.getUsername());
        credential.setPasswordHash(passwordEncoder.encode(bootstrapAdminProperties.getPassword()));
        credential.setRole(getOrCreateRole(Role.ADMIN));
        Credential savedCredential = credentialRepository.save(credential);

        UserData userData = savedCredential.getUserData();
        if (userData == null) {
            userData = byEmail.orElseGet(UserData::new);
        }
        userData.setCredential(savedCredential);
        userData.setEmail(bootstrapAdminProperties.getEmail());
        userDataRepository.save(userData);
    }

    private void validateUniqueUser(String username, String email) {
        if (credentialRepository.existsByUsername(username)) {
            throw new DuplicateUserException("Username is already taken");
        }
        if (userDataRepository.existsByEmail(email)) {
            throw new DuplicateUserException("Email is already taken");
        }
    }

    private RoleEntity getOrCreateRole(Role role) {
        return roleEntityRepository.findByRoleName(role)
                .orElseGet(() -> roleEntityRepository.save(RoleEntity.builder().roleName(role).build()));
    }

    private MeResponse toMeResponse(UserData userData) {
        return new MeResponse(
                userData.getUserId(),
                userData.getCredential().getUsername(),
                userData.getEmail(),
                userData.getPhone(),
                userData.getAddress(),
                userData.getCredential().getRole().getRoleName()
        );
    }
}

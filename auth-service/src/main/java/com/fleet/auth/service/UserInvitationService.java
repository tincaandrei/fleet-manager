package com.fleet.auth.service;

import com.fleet.auth.dto.AcceptInviteRequest;
import com.fleet.auth.dto.InviteValidationResponse;
import com.fleet.auth.dto.MessageResponse;
import com.fleet.auth.entity.Credential;
import com.fleet.auth.entity.InvitationToken;
import com.fleet.auth.entity.UserData;
import com.fleet.auth.entity.UserStatus;
import com.fleet.auth.exception.ApiStatusException;
import com.fleet.auth.repository.CredentialRepository;
import com.fleet.auth.repository.InvitationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserInvitationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final InvitationTokenRepository invitationTokenRepository;
    private final CredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.invitation.expiry-hours}")
    private long expiryHours;

    @Transactional
    public void createAndSendInvitation(UserData user, Long createdByAdminId) {
        invalidateUnusedTokens(user);
        String rawToken = generateToken();
        InvitationToken invitationToken = InvitationToken.builder()
                .user(user)
                .tokenHash(hashToken(rawToken))
                .expiresAt(Instant.now().plus(expiryHours, ChronoUnit.HOURS))
                .createdByAdminId(createdByAdminId)
                .build();
        invitationTokenRepository.save(invitationToken);
        mailService.sendUserInvitationEmail(user.getEmail(), setupLink(rawToken), expiryHours);
    }

    @Transactional
    public void createAndSendPasswordReset(UserData user, Long requestedByAdminId) {
        invalidateUnusedTokens(user);
        String rawToken = generateToken();
        InvitationToken invitationToken = InvitationToken.builder()
                .user(user)
                .tokenHash(hashToken(rawToken))
                .expiresAt(Instant.now().plus(expiryHours, ChronoUnit.HOURS))
                .createdByAdminId(requestedByAdminId)
                .build();
        invitationTokenRepository.save(invitationToken);
        mailService.sendPasswordResetEmail(user.getEmail(), setupLink(rawToken), expiryHours);
    }

    @Transactional
    public MessageResponse acceptInvite(AcceptInviteRequest request) {
        validatePassword(request.newPassword());
        InvitationToken invitationToken = requireUsableToken(request.token());
        UserData user = invitationToken.getUser();
        Credential credential = user.getCredential();

        credential.setPasswordHash(passwordEncoder.encode(request.newPassword().trim()));
        // Disabled accounts keep their password but stay disabled until an admin re-enables them.
        if (credential.getStatus() != UserStatus.DISABLED) {
            credential.setStatus(UserStatus.ACTIVE);
            credential.setEnabled(true);
        }
        credential.setPasswordChangeRequired(false);
        invitationToken.setUsedAt(Instant.now());

        credentialRepository.save(credential);
        invitationTokenRepository.save(invitationToken);
        return new MessageResponse("Password set successfully. You can now log in.");
    }

    @Transactional(readOnly = true)
    public InviteValidationResponse validateInvite(String token) {
        if (!StringUtils.hasText(token)) {
            return new InviteValidationResponse(false, null, "INVITATION_TOKEN_INVALID");
        }
        return invitationTokenRepository.findByTokenHash(hashToken(token))
                .map(invitationToken -> {
                    if (invitationToken.getUsedAt() != null) {
                        return new InviteValidationResponse(false, null, "INVITATION_TOKEN_USED");
                    }
                    if (invitationToken.getExpiresAt().isBefore(Instant.now())) {
                        return new InviteValidationResponse(false, null, "INVITATION_TOKEN_EXPIRED");
                    }
                    return new InviteValidationResponse(true, maskEmail(invitationToken.getUser().getEmail()), "VALID");
                })
                .orElseGet(() -> new InviteValidationResponse(false, null, "INVITATION_TOKEN_INVALID"));
    }

    @Transactional
    public void resendInvitation(UserData user, Long adminId) {
        Credential credential = user.getCredential();
        UserStatus status = credential.getStatus() == null ? UserStatus.ACTIVE : credential.getStatus();
        if (status != UserStatus.INVITED) {
            throw new ApiStatusException(HttpStatus.BAD_REQUEST, "INVITATION_NOT_ACCEPTED");
        }
        createAndSendInvitation(user, adminId);
    }

    private InvitationToken requireUsableToken(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            throw new ApiStatusException(HttpStatus.BAD_REQUEST, "INVITATION_TOKEN_INVALID");
        }
        InvitationToken invitationToken = invitationTokenRepository.findByTokenHash(hashToken(rawToken))
                .orElseThrow(() -> new ApiStatusException(HttpStatus.BAD_REQUEST, "INVITATION_TOKEN_INVALID"));
        if (invitationToken.getUsedAt() != null) {
            throw new ApiStatusException(HttpStatus.BAD_REQUEST, "INVITATION_TOKEN_USED");
        }
        if (invitationToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiStatusException(HttpStatus.BAD_REQUEST, "INVITATION_TOKEN_EXPIRED");
        }
        return invitationToken;
    }

    private void invalidateUnusedTokens(UserData user) {
        List<InvitationToken> activeTokens = invitationTokenRepository.findByUserAndUsedAtIsNull(user);
        Instant now = Instant.now();
        activeTokens.forEach(token -> token.setUsedAt(now));
        invitationTokenRepository.saveAll(activeTokens);
    }

    private String setupLink(String rawToken) {
        return frontendUrl.replaceAll("/+$", "") + "/accept-invite?token=" + rawToken;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private void validatePassword(String password) {
        if (!StringUtils.hasText(password)) {
            throw new ApiStatusException(HttpStatus.BAD_REQUEST, "PASSWORD_TOO_WEAK");
        }
        String trimmed = password.trim();
        boolean hasLetter = trimmed.chars().anyMatch(Character::isLetter);
        boolean hasDigit = trimmed.chars().anyMatch(Character::isDigit);
        if (trimmed.length() < 8 || !hasLetter || !hasDigit) {
            throw new ApiStatusException(HttpStatus.BAD_REQUEST, "PASSWORD_TOO_WEAK");
        }
    }

    private String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return null;
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String first = local.isEmpty() ? "*" : local.substring(0, 1);
        return first + "***@" + parts[1];
    }
}

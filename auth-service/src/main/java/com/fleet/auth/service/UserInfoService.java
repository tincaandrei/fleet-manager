package com.fleet.auth.service;

import com.fleet.auth.config.BootstrapAdminProperties;
import com.fleet.auth.dto.AssignBusinessUserRequest;
import com.fleet.auth.dto.BusinessRequest;
import com.fleet.auth.dto.BusinessResponse;
import com.fleet.auth.dto.CreateBusinessUserRequest;
import com.fleet.auth.entity.Credential;
import com.fleet.auth.dto.MeResponse;
import com.fleet.auth.dto.RegisterRequest;
import com.fleet.auth.dto.UpdateUserRequest;
import com.fleet.auth.dto.UserLookupResponse;
import com.fleet.auth.entity.Business;
import com.fleet.auth.entity.Role;
import com.fleet.auth.entity.RoleEntity;
import com.fleet.auth.entity.UserData;
import com.fleet.auth.exception.DuplicateUserException;
import com.fleet.auth.exception.ResourceNotFoundException;
import com.fleet.auth.repository.CredentialRepository;
import com.fleet.auth.repository.BusinessRepository;
import com.fleet.auth.repository.RoleEntityRepository;
import com.fleet.auth.repository.UserDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserInfoService implements UserDetailsService {

    private final BusinessRepository businessRepository;
    private final CredentialRepository credentialRepository;
    private final ProfileImageStorageService profileImageStorageService;
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
                .username(registerRequest.username().trim())
                .passwordHash(passwordEncoder.encode(registerRequest.password()))
                .role(getOrCreateRole(Role.EMPLOYEE))
                .build();
        Credential savedCredential = credentialRepository.save(credential);

        UserData userData = UserData.builder()
                .credential(savedCredential)
                .email(registerRequest.email().trim())
                .phone(normalizeOptional(registerRequest.phone()))
                .address(normalizeOptional(registerRequest.address()))
                .business(null)
                .build();

        UserData savedUserData = userDataRepository.save(userData);
        savedCredential.setUserData(savedUserData);
        return toMeResponse(savedUserData);
    }

    @Transactional
    public BusinessResponse createBusiness(BusinessRequest request, Authentication authentication) {
        requireSuperadmin(authentication);
        if (businessRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateUserException("Organization name is already registered");
        }
        Business business = Business.builder()
                .name(normalizeRequired(request.name()))
                .registrationNumber(normalizeOptional(request.registrationNumber()))
                .contactEmail(normalizeOptional(request.contactEmail()))
                .phone(normalizeOptional(request.phone()))
                .address(normalizeOptional(request.address()))
                .active(request.active() == null || request.active())
                .build();
        return toBusinessResponse(businessRepository.save(business));
    }

    @Transactional(readOnly = true)
    public List<BusinessResponse> listBusinesses(Authentication authentication) {
        requireSuperadmin(authentication);
        return businessRepository.findAll().stream()
                .map(this::toBusinessResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BusinessResponse getBusiness(Long businessId, Authentication authentication) {
        requireSuperadmin(authentication);
        return toBusinessResponse(getBusinessEntity(businessId));
    }

    @Transactional
    public BusinessResponse updateBusiness(Long businessId, BusinessRequest request, Authentication authentication) {
        requireSuperadmin(authentication);
        Business business = getBusinessEntity(businessId);
        business.setName(normalizeRequired(request.name()));
        business.setRegistrationNumber(normalizeOptional(request.registrationNumber()));
        business.setContactEmail(normalizeOptional(request.contactEmail()));
        business.setPhone(normalizeOptional(request.phone()));
        business.setAddress(normalizeOptional(request.address()));
        if (request.active() != null) {
            business.setActive(request.active());
        }
        return toBusinessResponse(businessRepository.save(business));
    }

    @Transactional
    public MeResponse createBusinessUser(Long businessId, CreateBusinessUserRequest request, Authentication authentication) {
        requireCanManageBusiness(businessId, authentication);
        if (request.role() == Role.SUPERADMIN) {
            throw new IllegalArgumentException("Organization users cannot have SUPERADMIN role");
        }
        validateUniqueUser(request.username(), request.email());
        Business business = getBusinessEntity(businessId);
        if (!business.isActive()) {
            throw new IllegalArgumentException("Organization is inactive");
        }

        Credential credential = Credential.builder()
                .username(request.username().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(getOrCreateRole(request.role()))
                .build();
        Credential savedCredential = credentialRepository.save(credential);

        UserData userData = UserData.builder()
                .credential(savedCredential)
                .email(request.email().trim())
                .phone(normalizeOptional(request.phone()))
                .address(normalizeOptional(request.address()))
                .business(business)
                .build();

        UserData savedUserData = userDataRepository.save(userData);
        savedCredential.setUserData(savedUserData);
        return toMeResponse(savedUserData);
    }

    @Transactional
    public MeResponse updateRole(Long businessId, Long userId, Role role, Authentication authentication) {
        requireCanManageBusiness(businessId, authentication);
        if (role == Role.SUPERADMIN) {
            throw new IllegalArgumentException("Organization users cannot have SUPERADMIN role");
        }
        UserData userData = userDataRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        assertSameBusiness(businessId, userData);
        userData.getCredential().setRole(getOrCreateRole(role));
        credentialRepository.save(userData.getCredential());
        return toMeResponse(userData);
    }

    @Transactional(readOnly = true)
    public List<MeResponse> listBusinessUsers(Long businessId, Authentication authentication) {
        requireCanManageBusiness(businessId, authentication);
        return userDataRepository.findByBusinessIdOrderByCredentialUsernameAsc(businessId).stream()
                .map(this::toMeResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MeResponse> listUnassignedUsers(Authentication authentication) {
        requireSuperadmin(authentication);
        return userDataRepository.findByBusinessIsNullOrderByCredentialUsernameAsc().stream()
                .filter(userData -> userData.getCredential().getRole().getRoleName().canonical() != Role.SUPERADMIN)
                .map(this::toMeResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserLookupResponse> lookupUsers(List<Long> ids, Authentication authentication) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Set<Long> uniqueIds = ids.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toUnmodifiableSet());
        if (uniqueIds.isEmpty()) {
            return List.of();
        }

        Long currentUserId = currentUserId(authentication);
        Long currentBusinessId = currentBusinessId(authentication);
        boolean superadmin = hasRole(authentication, Role.SUPERADMIN);
        boolean businessAdmin = hasRole(authentication, Role.BUSINESS_ADMIN);

        return userDataRepository.findByUserIdIn(List.copyOf(uniqueIds)).stream()
                .filter(userData -> canLookupUser(userData, currentUserId, currentBusinessId, superadmin, businessAdmin))
                .map(this::toUserLookupResponse)
                .toList();
    }

    @Transactional
    public MeResponse assignUnassignedUser(Long userId, AssignBusinessUserRequest request, Authentication authentication) {
        requireSuperadmin(authentication);
        if (request.role() == Role.SUPERADMIN) {
            throw new IllegalArgumentException("Organization users cannot have SUPERADMIN role");
        }

        UserData userData = getUserData(userId);
        if (userData.getCredential().getRole().getRoleName().canonical() == Role.SUPERADMIN) {
            throw new IllegalArgumentException("SUPERADMIN users cannot be assigned to an organization");
        }
        if (userData.getBusiness() != null) {
            throw new IllegalArgumentException("User is already assigned to an organization");
        }

        Business business = getBusinessEntity(request.businessId());
        if (!business.isActive()) {
            throw new IllegalArgumentException("Organization is inactive");
        }

        userData.setBusiness(business);
        userData.getCredential().setRole(getOrCreateRole(request.role()));
        credentialRepository.save(userData.getCredential());
        return toMeResponse(userDataRepository.save(userData));
    }

    @Transactional(readOnly = true)
    public MeResponse getCurrentUser(String username) {
        UserData userData = userDataRepository.findByCredentialUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return toMeResponse(userData);
    }

    @Transactional
    public MeResponse updateCurrentUser(UpdateUserRequest request, Authentication authentication) {
        UserData userData = userDataRepository.findByCredentialUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));
        applyUserUpdate(userData, request);
        return toMeResponse(userDataRepository.save(userData));
    }

    @Transactional
    public MeResponse uploadCurrentUserProfileImage(MultipartFile file, Authentication authentication) {
        UserData userData = userDataRepository.findByCredentialUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));
        StoredProfileImage image = profileImageStorageService.save(file);
        profileImageStorageService.deleteQuietly(userData.getProfileImageStoragePath());
        userData.setProfileImageStoragePath(image.storagePath());
        userData.setProfileImageOriginalFileName(image.originalFileName());
        userData.setProfileImageContentType(image.contentType());
        userData.setProfileImageFileSize(image.fileSize());
        return toMeResponse(userDataRepository.save(userData));
    }

    @Transactional
    public MeResponse deleteCurrentUserProfileImage(Authentication authentication) {
        UserData userData = userDataRepository.findByCredentialUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));
        profileImageStorageService.deleteQuietly(userData.getProfileImageStoragePath());
        userData.setProfileImageStoragePath(null);
        userData.setProfileImageOriginalFileName(null);
        userData.setProfileImageContentType(null);
        userData.setProfileImageFileSize(null);
        return toMeResponse(userDataRepository.save(userData));
    }

    @Transactional(readOnly = true)
    public ProfileImageResource loadCurrentUserProfileImage(Authentication authentication) {
        UserData userData = userDataRepository.findByCredentialUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));
        return loadProfileImage(userData);
    }

    @Transactional(readOnly = true)
    public ProfileImageResource loadUserProfileImage(Long userId, Authentication authentication) {
        UserData userData = getUserData(userId);
        if (!userData.getUserId().equals(currentUserId(authentication))) {
            requireCanManageUser(userData, authentication);
        }
        return loadProfileImage(userData);
    }

    private ProfileImageResource loadProfileImage(UserData userData) {
        if (!StringUtils.hasText(userData.getProfileImageStoragePath())) {
            throw new ResourceNotFoundException("Profile image not found");
        }
        return new ProfileImageResource(
                profileImageStorageService.load(userData.getProfileImageStoragePath()),
                userData.getProfileImageContentType(),
                userData.getProfileImageOriginalFileName()
        );
    }

    @Transactional
    public MeResponse updateUser(Long userId, UpdateUserRequest request, Authentication authentication) {
        requireSuperadmin(authentication);
        UserData userData = getUserData(userId);
        applyUserUpdate(userData, request);
        return toMeResponse(userDataRepository.save(userData));
    }

    @Transactional
    public MeResponse updateBusinessUser(Long businessId, Long userId, UpdateUserRequest request, Authentication authentication) {
        requireCanManageBusiness(businessId, authentication);
        UserData userData = getUserData(userId);
        assertSameBusiness(businessId, userData);
        applyUserUpdate(userData, request);
        return toMeResponse(userDataRepository.save(userData));
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
        credential.setRole(getOrCreateRole(Role.SUPERADMIN));
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

    private UserData getUserData(Long userId) {
        return userDataRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private void applyUserUpdate(UserData userData, UpdateUserRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (StringUtils.hasText(request.email())) {
            String email = request.email().trim();
            userDataRepository.findByEmail(email)
                    .filter(existing -> !existing.getUserId().equals(userData.getUserId()))
                    .ifPresent(existing -> {
                        throw new DuplicateUserException("Email is already taken");
                    });
            userData.setEmail(email);
        }
        userData.setPhone(normalizeOptional(request.phone()));
        userData.setAddress(normalizeOptional(request.address()));
        if (StringUtils.hasText(request.password())) {
            String password = request.password().trim();
            if (password.length() < 6 || password.length() > 255) {
                throw new IllegalArgumentException("Password must be between 6 and 255 characters");
            }
            userData.getCredential().setPasswordHash(passwordEncoder.encode(password));
            credentialRepository.save(userData.getCredential());
        }
    }

    private void requireSuperadmin(Authentication authentication) {
        if (!hasRole(authentication, Role.SUPERADMIN)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private void requireCanManageBusiness(Long businessId, Authentication authentication) {
        if (hasRole(authentication, Role.SUPERADMIN)) {
            return;
        }
        if (!hasRole(authentication, Role.BUSINESS_ADMIN) || !businessId.equals(currentBusinessId(authentication))) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private void requireCanManageUser(UserData userData, Authentication authentication) {
        if (hasRole(authentication, Role.SUPERADMIN)) {
            return;
        }
        Business business = userData.getBusiness();
        if (business == null || !hasRole(authentication, Role.BUSINESS_ADMIN)
                || !business.getId().equals(currentBusinessId(authentication))) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private boolean canLookupUser(UserData userData,
                                  Long currentUserId,
                                  Long currentBusinessId,
                                  boolean superadmin,
                                  boolean businessAdmin) {
        if (superadmin) {
            return true;
        }
        if (userData.getUserId().equals(currentUserId)) {
            return true;
        }
        Business business = userData.getBusiness();
        return businessAdmin
                && business != null
                && currentBusinessId != null
                && currentBusinessId.equals(business.getId());
    }

    private boolean hasRole(Authentication authentication, Role role) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(role.asAuthority()));
    }

    private Long currentBusinessId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        if (authentication.getPrincipal() instanceof CredentialDetails details) {
            return details.getBusinessId();
        }
        return userDataRepository.findByCredentialUsername(authentication.getName())
                .map(UserData::getBusiness)
                .map(Business::getId)
                .orElse(null);
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        if (authentication.getPrincipal() instanceof CredentialDetails details) {
            return details.getUserId();
        }
        return userDataRepository.findByCredentialUsername(authentication.getName())
                .map(UserData::getUserId)
                .orElse(null);
    }

    private Business getBusinessEntity(Long businessId) {
        return businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + businessId));
    }

    private void assertSameBusiness(Long businessId, UserData userData) {
        if (userData.getBusiness() == null || !businessId.equals(userData.getBusiness().getId())) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private RoleEntity getOrCreateRole(Role role) {
        return roleEntityRepository.findByRoleName(role)
                .orElseGet(() -> roleEntityRepository.save(RoleEntity.builder().roleName(role).build()));
    }

    private MeResponse toMeResponse(UserData userData) {
        Business business = userData.getBusiness();
        String profileImageUrl = null;
        if (StringUtils.hasText(userData.getProfileImageStoragePath())) {
            String imageVersion = Path.of(userData.getProfileImageStoragePath()).getFileName().toString();
            profileImageUrl = "/api/auth/users/" + userData.getUserId() + "/profile-image?v=" + imageVersion;
        }
        return new MeResponse(
                userData.getUserId(),
                userData.getCredential().getUsername(),
                userData.getEmail(),
                userData.getPhone(),
                userData.getAddress(),
                userData.getCredential().getRole().getRoleName().canonical(),
                business == null ? null : business.getId(),
                business == null ? null : business.getName(),
                profileImageUrl,
                userData.getProfileImageOriginalFileName()
        );
    }

    private UserLookupResponse toUserLookupResponse(UserData userData) {
        Business business = userData.getBusiness();
        return new UserLookupResponse(
                userData.getUserId(),
                userData.getCredential().getUsername(),
                userData.getEmail(),
                business == null ? null : business.getId(),
                userData.getCredential().getRole().getRoleName().canonical()
        );
    }

    public record ProfileImageResource(Resource resource, String contentType, String originalFileName) {
    }

    private BusinessResponse toBusinessResponse(Business business) {
        return new BusinessResponse(
                business.getId(),
                business.getName(),
                business.getRegistrationNumber(),
                business.getContactEmail(),
                business.getPhone(),
                business.getAddress(),
                business.isActive(),
                business.getCreatedAt(),
                business.getUpdatedAt()
        );
    }

    private String normalizeRequired(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Required value is missing");
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

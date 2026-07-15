package swp391.aistudyhub.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.aistudyhub.config.JwtProperties;
import swp391.aistudyhub.dto.request.ChangePasswordRequest;
import swp391.aistudyhub.dto.request.ForgotPasswordRequest;
import swp391.aistudyhub.dto.request.LoginRequest;
import swp391.aistudyhub.dto.request.RefreshTokenRequest;
import swp391.aistudyhub.dto.request.RegisterRequest;
import swp391.aistudyhub.dto.request.ResetPasswordRequest;
import swp391.aistudyhub.dto.request.UpdateProfileRequest;
import swp391.aistudyhub.dto.response.AuthResponse;
import swp391.aistudyhub.dto.response.UserProfileResponse;
import swp391.aistudyhub.entity.AdminProfile;
import swp391.aistudyhub.entity.CloudStorage;
import swp391.aistudyhub.entity.CustomerProfile;
import swp391.aistudyhub.entity.ModeratorProfile;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.entity.UserSession;
import swp391.aistudyhub.enums.AccountStatus;
import swp391.aistudyhub.enums.UserRole;
import swp391.aistudyhub.exception.AuthException;
import swp391.aistudyhub.repository.AdminProfileRepository;
import swp391.aistudyhub.repository.CloudStorageRepository;
import swp391.aistudyhub.repository.CustomerProfileRepository;
import swp391.aistudyhub.repository.ModeratorProfileRepository;
import swp391.aistudyhub.repository.UserRepository;
import swp391.aistudyhub.repository.UserSessionRepository;
import swp391.aistudyhub.security.CustomUserDetails;
import swp391.aistudyhub.security.JwtService;
import swp391.aistudyhub.service.AuthService;
import swp391.aistudyhub.service.MailService;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final long DEFAULT_STORAGE_QUOTA = 5_368_709_120L;

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final ModeratorProfileRepository moderatorProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final MailService mailService;
    private final CloudStorageRepository cloudStorageRepository;

    @Value("${app.frontend.reset-password-url:http://localhost:5173/reset-password}")
    private String resetPasswordUrl;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw AuthException.conflict("Email is already registered");
        }

        UserRole role = request.getRole() != null ? request.getRole() : UserRole.CUSTOMER;

        User user = new User();
        user.setEmail(email);

        /*
         * Project hiện tại đang dùng NoOpPasswordEncoder để demo,
         * nên giá trị encode vẫn tương thích với plain text.
         */
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        user.setRole(role);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setCreatedAt(Instant.now());

        user = userRepository.save(user);

        createRoleProfile(user, request);
        createDefaultCloudStorage(user);

        return buildAuthResponse(user, null, null);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String email = normalizeEmail(request.getEmail());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> AuthException.unauthorized("Invalid email or password"));

        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        String refreshToken = jwtService.generateRefreshToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        saveSession(user, refreshToken, request.getDeviceInfo(), resolveClientIp(httpRequest));

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtService.isTokenValid(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            throw AuthException.unauthorized("Invalid or expired refresh token");
        }

        UserSession session = userSessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> AuthException.unauthorized("Session not found"));

        if (session.getExpiresAt().isBefore(Instant.now())) {
            userSessionRepository.delete(session);
            throw AuthException.unauthorized("Session expired");
        }

        User user = session.getUser();

        String newAccessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        String newRefreshToken = jwtService.generateRefreshToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        userSessionRepository.delete(session);
        saveSession(user, newRefreshToken, session.getDeviceInfo(), session.getIpAddress());

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request) {
        userSessionRepository.deleteByRefreshToken(request.getRefreshToken());
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());

        userRepository.findByEmailIgnoreCase(email)
                .ifPresent(user -> {
                    String token = jwtService.generateResetToken(user.getId(), user.getEmail());
                    String resetLink = resetPasswordUrl + "?token=" + token;
                    mailService.sendPasswordResetEmail(user.getEmail(), resetLink);
                });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String token = request.getToken();

        if (!jwtService.isTokenValid(token) || !jwtService.isResetToken(token)) {
            throw AuthException.badRequest("Invalid or expired reset token");
        }

        User user = userRepository.findById(jwtService.extractUserId(token))
                .orElseThrow(() -> AuthException.notFound("User not found"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        userSessionRepository.deleteByUser_Id(user.getId());
    }

    @Override
    @Transactional
    public void changePassword(CustomUserDetails currentUser, ChangePasswordRequest request) {
        if (currentUser == null) {
            throw AuthException.unauthorized("You are not login yet!");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> AuthException.notFound("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw AuthException.badRequest("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        userSessionRepository.deleteByUser_Id(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(CustomUserDetails currentUser) {
        if (currentUser == null) {
            throw AuthException.unauthorized("You are not login yet!");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> AuthException.notFound("User not found"));

        return mapToProfile(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(CustomUserDetails currentUser, UpdateProfileRequest request) {
        if (currentUser == null) {
            throw AuthException.unauthorized("You are not login yet!");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> AuthException.notFound("User not found"));

        switch (user.getRole()) {
            case CUSTOMER -> updateCustomerProfile(user, request);
            case ADMIN -> updateAdminProfile(user, request);
            case MODERATOR -> updateModeratorProfile(user, request);
            default -> throw AuthException.badRequest("Unsupported role for profile update");
        }

        return mapToProfile(user);
    }

    private void createRoleProfile(User user, RegisterRequest request) {
        switch (user.getRole()) {
            case CUSTOMER -> {
                CustomerProfile profile = new CustomerProfile();
                profile.setUser(user);
                profile.setFullName(request.getFullName());
                profile.setStudentCode(request.getStudentCode());
                profile.setSchoolName(request.getSchoolName());
                customerProfileRepository.save(profile);
            }
            case ADMIN -> {
                AdminProfile profile = new AdminProfile();
                profile.setUser(user);
                profile.setFullName(request.getFullName());
                profile.setAccessLevel(1);
                adminProfileRepository.save(profile);
            }
            case MODERATOR -> {
                ModeratorProfile profile = new ModeratorProfile();
                profile.setUser(user);
                profile.setFullName(request.getFullName());
                profile.setDepartment(request.getDepartment());
                profile.setAssignedSubject(request.getAssignedSubject());
                moderatorProfileRepository.save(profile);
            }
            default -> throw AuthException.badRequest("Invalid role: " + user.getRole());
        }
    }

    private void createDefaultCloudStorage(User user) {
        boolean existed = cloudStorageRepository.findByUser_Id(user.getId()).isPresent();

        if (existed) {
            return;
        }

        CloudStorage storage = new CloudStorage();
        storage.setUser(user);
        storage.setTotalQuota(DEFAULT_STORAGE_QUOTA);
        storage.setUsedQuota(0L);

        cloudStorageRepository.save(storage);
    }

    private void updateCustomerProfile(User user, UpdateProfileRequest request) {
        CustomerProfile profile = customerProfileRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> AuthException.notFound("Customer profile not found"));

        applyIfPresent(request.getFullName(), profile::setFullName);
        applyIfPresent(request.getStudentCode(), profile::setStudentCode);
        applyIfPresent(request.getSchoolName(), profile::setSchoolName);

        customerProfileRepository.save(profile);
    }

    private void updateAdminProfile(User user, UpdateProfileRequest request) {
        AdminProfile profile = adminProfileRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> AuthException.notFound("Admin profile not found"));

        applyIfPresent(request.getFullName(), profile::setFullName);

        adminProfileRepository.save(profile);
    }

    private void updateModeratorProfile(User user, UpdateProfileRequest request) {
        ModeratorProfile profile = moderatorProfileRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> AuthException.notFound("Moderator profile not found"));

        applyIfPresent(request.getFullName(), profile::setFullName);
        applyIfPresent(request.getDepartment(), profile::setDepartment);
        applyIfPresent(request.getAssignedSubject(), profile::setAssignedSubject);

        moderatorProfileRepository.save(profile);
    }

    private void applyIfPresent(String value, java.util.function.Consumer<String> setter) {
        if (value != null && !value.isBlank()) {
            setter.accept(value.trim());
        }
    }

    private void saveSession(
            User user,
            String refreshToken,
            String deviceInfo,
            String ipAddress
    ) {
        UserSession session = new UserSession();
        session.setUser(user);
        session.setRefreshToken(refreshToken);
        session.setDeviceInfo(deviceInfo);
        session.setIpAddress(ipAddress);
        session.setExpiresAt(Instant.now().plusMillis(jwtProperties.getRefreshExpirationMs()));

        userSessionRepository.save(session);
    }

    private AuthResponse buildAuthResponse(
            User user,
            String accessToken,
            String refreshToken
    ) {
        if (accessToken == null) {
            accessToken = jwtService.generateAccessToken(
                    user.getId(),
                    user.getEmail(),
                    user.getRole().name()
            );
        }

        if (refreshToken == null) {
            refreshToken = jwtService.generateRefreshToken(
                    user.getId(),
                    user.getEmail(),
                    user.getRole().name()
            );

            saveSession(user, refreshToken, null, null);
        }

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInMs(jwtProperties.getAccessExpirationMs())
                .user(mapToProfile(user))
                .build();
    }

    private UserProfileResponse mapToProfile(User user) {
        UserProfileResponse.UserProfileResponseBuilder builder =
                UserProfileResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .accountStatus(user.getAccountStatus())
                        .createdAt(user.getCreatedAt());

        switch (user.getRole()) {
            case CUSTOMER -> customerProfileRepository.findByUser_Id(user.getId())
                    .ifPresent(profile -> {
                        builder.fullName(profile.getFullName());
                        builder.studentCode(profile.getStudentCode());
                        builder.schoolName(profile.getSchoolName());
                    });

            case ADMIN -> adminProfileRepository.findByUser_Id(user.getId())
                    .ifPresent(profile -> {
                        builder.fullName(profile.getFullName());
                        builder.accessLevel(profile.getAccessLevel());
                    });

            case MODERATOR -> moderatorProfileRepository.findByUser_Id(user.getId())
                    .ifPresent(profile -> {
                        builder.fullName(profile.getFullName());
                        builder.department(profile.getDepartment());
                        builder.assignedSubject(profile.getAssignedSubject());
                    });

            default -> {
            }
        }

        return builder.build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String forwarded = request.getHeader("X-Forwarded-For");

        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }

        return email.trim().toLowerCase();
    }
}
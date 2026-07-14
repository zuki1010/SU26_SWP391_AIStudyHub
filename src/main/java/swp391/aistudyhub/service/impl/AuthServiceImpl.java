package swp391.aistudyhub.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final ModeratorProfileRepository moderatorProfileRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final MailService mailService;
    private final CloudStorageRepository cloudStorageRepository;

    @Value("${app.frontend.reset-password-url:http://localhost:3000/reset-password}")
    private String resetPasswordUrl;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw AuthException.conflict("Email is already registered");
        }

        /*
         * RegisterRequest.getRole() của project bạn là enum UserRole,
         * nên phải dùng .name(), không dùng .toUpperCase().
         *
         * Lưu DB dạng:
         * CUSTOMER / ADMIN / MODERATOR
         *
         * Không lưu:
         * ROLE_CUSTOMER
         */
        String role = request.getRole() != null
                ? request.getRole().name()
                : "CUSTOMER";

        User user = new User();
        user.setEmail(email);

        /*
         * Project hiện đang dùng NoOpPasswordEncoder / plain text để demo.
         * Sau này nếu đổi sang BCrypt thì sửa lại chỗ này.
         */
        user.setPasswordHash(request.getPassword());

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
        String email = request.getEmail().trim().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> AuthException.unauthorized("Invalid email or password"));

        String role = normalizeRole(user.getRole());

        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                role
        );

        String refreshToken = jwtService.generateRefreshToken(
                user.getId(),
                user.getEmail(),
                role
        );

        saveSession(
                user,
                refreshToken,
                request.getDeviceInfo(),
                resolveClientIp(httpRequest)
        );

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
        String role = normalizeRole(user.getRole());

        String newAccessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                role
        );

        String newRefreshToken = jwtService.generateRefreshToken(
                user.getId(),
                user.getEmail(),
                role
        );

        userSessionRepository.delete(session);

        saveSession(
                user,
                newRefreshToken,
                session.getDeviceInfo(),
                session.getIpAddress()
        );

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request) {
        userSessionRepository.deleteByRefreshToken(request.getRefreshToken());
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();

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

        UUID userId = jwtService.extractUserId(token);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> AuthException.notFound("User not found"));

        /*
         * Project đang dùng plain text / NoOpPasswordEncoder.
         */
        user.setPasswordHash(request.getNewPassword());

        userRepository.save(user);
        userSessionRepository.deleteByUser_Id(userId);
    }

    @Override
    @Transactional
    public void changePassword(CustomUserDetails currentUser, ChangePasswordRequest request) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> AuthException.notFound("User not found"));

        /*
         * Vì project hiện dùng plain text nên so sánh trực tiếp.
         * Nếu sau này chuyển BCrypt thì đổi sang passwordEncoder.matches(...).
         */
        if (!request.getCurrentPassword().equals(user.getPasswordHash())) {
            throw AuthException.badRequest("Current password is incorrect");
        }

        user.setPasswordHash(request.getNewPassword());

        userRepository.save(user);
        userSessionRepository.deleteByUser_Id(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(CustomUserDetails currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> AuthException.notFound("User not found"));

        return mapToProfile(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(CustomUserDetails currentUser, UpdateProfileRequest request) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> AuthException.notFound("User not found"));

        String role = normalizeRole(user.getRole());

        switch (role) {
            case "CUSTOMER" -> updateCustomerProfile(user, request);
            case "ADMIN" -> updateAdminProfile(user, request);
            case "MODERATOR" -> updateModeratorProfile(user, request);
            default -> throw AuthException.badRequest("Unsupported role for profile update");
        }

        return mapToProfile(user);
    }

    private void createRoleProfile(User user, RegisterRequest request) {
        String role = normalizeRole(user.getRole());

        switch (role) {
            case "CUSTOMER" -> {
                CustomerProfile profile = new CustomerProfile();
                profile.setUser(user);
                profile.setFullName(request.getFullName());
                profile.setStudentCode(request.getStudentCode());
                profile.setSchoolName(request.getSchoolName());

                customerProfileRepository.save(profile);
            }
            case "ADMIN" -> {
                AdminProfile profile = new AdminProfile();
                profile.setUser(user);
                profile.setFullName(request.getFullName());
                profile.setAccessLevel(1);

                adminProfileRepository.save(profile);
            }
            case "MODERATOR" -> {
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
        CloudStorage storage = new CloudStorage();
        storage.setUser(user);
        storage.setTotalQuota(5368709120L);
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

    private void saveSession(User user, String refreshToken, String deviceInfo, String ipAddress) {
        UserSession session = new UserSession();

        session.setUser(user);
        session.setRefreshToken(refreshToken);
        session.setDeviceInfo(deviceInfo);
        session.setIpAddress(ipAddress);
        session.setExpiresAt(Instant.now().plusMillis(jwtProperties.getRefreshExpirationMs()));

        userSessionRepository.save(session);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        String role = normalizeRole(user.getRole());

        if (accessToken == null) {
            accessToken = jwtService.generateAccessToken(
                    user.getId(),
                    user.getEmail(),
                    role
            );
        }

        if (refreshToken == null) {
            refreshToken = jwtService.generateRefreshToken(
                    user.getId(),
                    user.getEmail(),
                    role
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
        String role = normalizeRole(user.getRole());

        UserProfileResponse.UserProfileResponseBuilder builder = UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(role)
                .accountStatus(user.getAccountStatus())
                .createdAt(user.getCreatedAt());

        switch (role) {
            case "CUSTOMER" -> customerProfileRepository.findByUser_Id(user.getId()).ifPresent(profile -> {
                builder.fullName(profile.getFullName());
                builder.studentCode(profile.getStudentCode());
                builder.schoolName(profile.getSchoolName());
            });
            case "ADMIN" -> adminProfileRepository.findByUser_Id(user.getId()).ifPresent(profile -> {
                builder.fullName(profile.getFullName());
                builder.accessLevel(profile.getAccessLevel());
            });
            case "MODERATOR" -> moderatorProfileRepository.findByUser_Id(user.getId()).ifPresent(profile -> {
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

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "CUSTOMER";
        }

        String normalizedRole = role.trim().toUpperCase();

        if (normalizedRole.startsWith("ROLE_")) {
            normalizedRole = normalizedRole.substring(5);
        }

        return normalizedRole;
    }
}
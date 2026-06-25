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
import swp391.aistudyhub.dto.request.*;
import swp391.aistudyhub.dto.response.AuthResponse;
import swp391.aistudyhub.dto.response.UserProfileResponse;
import swp391.aistudyhub.entity.*;
import swp391.aistudyhub.enums.AccountStatus;
import swp391.aistudyhub.exception.AuthException;
import swp391.aistudyhub.repository.*;
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
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final MailService mailService;

    @Value("${app.frontend.reset-password-url:http://localhost:3000/reset-password}")
    private String resetPasswordUrl;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw AuthException.conflict("Email is already registered");
        }

        String role = request.getRole() != null ? request.getRole().toUpperCase() : "CUSTOMER";

        User user = new User();
//        user.setId(UUID.randomUUID());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPasswordHash(request.getPassword());
        user.setRole("ROLE_" + role);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setCreatedAt(Instant.now());
        user = userRepository.save(user);

        createRoleProfile(user, request);

        return buildAuthResponse(user, null, null);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().trim().toLowerCase(),
                        request.getPassword()));

        User user = userRepository.findByEmailIgnoreCase(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> AuthException.unauthorized("Invalid email or password"));

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail(), user.getRole());

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
        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String newRefreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail(), user.getRole());

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
        userRepository.findByEmailIgnoreCase(request.getEmail().trim().toLowerCase())
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

        user.setPasswordHash(request.getNewPassword());
        userRepository.save(user);
        userSessionRepository.deleteByUser_Id(userId);
    }

    @Override
    @Transactional
    public void changePassword(CustomUserDetails currentUser, ChangePasswordRequest request) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> AuthException.notFound("User not found"));

        if (!request.getCurrentPassword().equals(user.getPasswordHash())) {
    throw AuthException.badRequest("Current password is incorrect");
    }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
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

        switch (user.getRole()) {
            case "CUSTOMER" -> updateCustomerProfile(user, request);
            case "ADMIN" -> updateAdminProfile(user, request);
            case "MODERATOR" -> updateModeratorProfile(user, request);
            default -> throw AuthException.badRequest("Unsupported role for profile update");
        }

        return mapToProfile(user);
    }

    private void createRoleProfile(User user, RegisterRequest request) {
        switch (user.getRole()) {
            case "CUSTOMER" -> {
                CustomerProfile profile = new CustomerProfile();
//                profile.setId(UUID.randomUUID());
                profile.setUser(user);
                profile.setFullName(request.getFullName());
                profile.setStudentCode(request.getStudentCode());
                profile.setSchoolName(request.getSchoolName());
                customerProfileRepository.save(profile);
            }
            case "ADMIN" -> {
                AdminProfile profile = new AdminProfile();
//                profile.setId(UUID.randomUUID());
                profile.setUser(user);
                profile.setFullName(request.getFullName());
                profile.setAccessLevel(1);
                adminProfileRepository.save(profile);
            }
            case "MODERATOR" -> {
                ModeratorProfile profile = new ModeratorProfile();
//                profile.setId(UUID.randomUUID());
                profile.setUser(user);
                profile.setFullName(request.getFullName());
                profile.setDepartment(request.getDepartment());
                profile.setAssignedSubject(request.getAssignedSubject());
                moderatorProfileRepository.save(profile);
            }
            default -> throw AuthException.badRequest("Invalid role: " + user.getRole());
        }
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
//        session.setId(UUID.randomUUID());
        session.setUser(user);
        session.setRefreshToken(refreshToken);
        session.setDeviceInfo(deviceInfo);
        session.setIpAddress(ipAddress);
        session.setExpiresAt(Instant.now().plusMillis(jwtProperties.getRefreshExpirationMs()));
        userSessionRepository.save(session);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        if (accessToken == null) {
            accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        }
        if (refreshToken == null) {
            refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail(), user.getRole());
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
        UserProfileResponse.UserProfileResponseBuilder builder = UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .accountStatus(user.getAccountStatus())
                .createdAt(user.getCreatedAt());

        switch (user.getRole()) {
            case "CUSTOMER" -> customerProfileRepository.findByUser_Id(user.getId()).ifPresent(p -> {
                builder.fullName(p.getFullName());
                builder.studentCode(p.getStudentCode());
                builder.schoolName(p.getSchoolName());
            });
            case "ADMIN" -> adminProfileRepository.findByUser_Id(user.getId()).ifPresent(p -> {
                builder.fullName(p.getFullName());
                builder.accessLevel(p.getAccessLevel());
            });
            case "MODERATOR" -> moderatorProfileRepository.findByUser_Id(user.getId()).ifPresent(p -> {
                builder.fullName(p.getFullName());
                builder.department(p.getDepartment());
                builder.assignedSubject(p.getAssignedSubject());
            });
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
}

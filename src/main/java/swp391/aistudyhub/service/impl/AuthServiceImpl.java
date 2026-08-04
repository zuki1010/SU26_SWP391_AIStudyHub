package swp391.aistudyhub.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
import swp391.aistudyhub.enums.UserRole;
import swp391.aistudyhub.exception.AuthException;
import swp391.aistudyhub.repository.*;
import swp391.aistudyhub.security.CustomUserDetails;
import swp391.aistudyhub.security.JwtService;
import swp391.aistudyhub.service.AuthService;
import swp391.aistudyhub.service.MailService;

import java.security.SecureRandom;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw AuthException.conflict("Email is already registered");
        }

        UserRole role = request.getRole() != null ? request.getRole() : UserRole.CUSTOMER;

        String verifyToken = generateOtp();

        User user = new User();
        user.setEmail(email);

        // Mã hóa mật khẩu khi đăng ký.
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        user.setRole(role);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setCreatedAt(Instant.now());

        user.setEmailVerified(false);
        user.setEmailVerificationToken(verifyToken);
        user.setEmailVerificationExpiredAt(Instant.now().plusSeconds(15 * 60));

        user = userRepository.save(user);

        createRoleProfile(user, request);

        CloudStorage storage = new CloudStorage();
        storage.setUser(user);

        SystemConfig systemConfig = systemConfigRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("This config is not found!"));

        storage.setTotalQuota(systemConfig.getTotalStorageQuotaGb());
        storage.setUsedQuota(0.0);
        cloudStorageRepository.save(storage);

        mailService.sendVerificationEmail(user.getEmail(), verifyToken);

        return AuthResponse.builder()
                .accessToken(null)
                .refreshToken(null)
                .tokenType("Bearer")
                .expiresInMs(0)
                .user(mapToProfile(user))
                .build();
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

        if (!user.isEmailVerified()) {
            throw AuthException.forbidden("Please verify your email before login.");
        }

        /*
         * Nếu user cũ đang lưu mật khẩu plain text,
         * sau khi login thành công thì tự đổi sang BCrypt.
         */
        if (!isBcryptHash(user.getPasswordHash())) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            userRepository.save(user);
        }

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
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> AuthException.badRequest("Invalid verification token."));

        if (user.getEmailVerificationExpiredAt() == null ||
                user.getEmailVerificationExpiredAt().isBefore(Instant.now())) {
            throw AuthException.badRequest("Verification token has expired.");
        }

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpiredAt(null);

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmailIgnoreCase(email.trim().toLowerCase())
                .orElseThrow(() -> AuthException.notFound("Email does not exist."));

        if (user.isEmailVerified()) {
            throw AuthException.badRequest("Email is already verified.");
        }

        String verifyToken = generateOtp();

        user.setEmailVerificationToken(verifyToken);
        user.setEmailVerificationExpiredAt(Instant.now().plusSeconds(15 * 60));

        userRepository.save(user);

        mailService.sendVerificationEmail(user.getEmail(), verifyToken);
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

        if (!user.isEmailVerified()) {
            throw AuthException.forbidden("Please verify your email before refreshing token.");
        }

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
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        userRepository.findByEmailIgnoreCase(email)
                .ifPresent(user -> {
                    String otp = generateOtp();

                    user.setPasswordResetOtp(otp);
                    user.setPasswordResetExpiredAt(Instant.now().plusSeconds(15 * 60));

                    userRepository.save(user);

                    mailService.sendPasswordResetEmail(user.getEmail(), otp);
                });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String otp = request.getToken().trim();

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> AuthException.notFound("Email does not exist."));

        if (user.getPasswordResetOtp() == null ||
                !user.getPasswordResetOtp().equals(otp)) {
            throw AuthException.badRequest("Invalid OTP.");
        }

        if (user.getPasswordResetExpiredAt() == null ||
                user.getPasswordResetExpiredAt().isBefore(Instant.now())) {
            throw AuthException.badRequest("OTP has expired.");
        }

        // Mã hóa mật khẩu mới khi reset password.
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));

        user.setPasswordResetOtp(null);
        user.setPasswordResetExpiredAt(null);

        userRepository.save(user);

        userSessionRepository.deleteByUser_Id(user.getId());
    }

    @Override
    @Transactional
    public void changePassword(CustomUserDetails currentUser, ChangePasswordRequest request) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> AuthException.notFound("User not found"));

        /*
         * Không so sánh plain text bằng equals nữa.
         * Phải dùng passwordEncoder.matches().
         */
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw AuthException.badRequest("Current password is incorrect");
        }

        // Mã hóa mật khẩu mới khi change password.
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

        switch (user.getRole().name()) {
            case "CUSTOMER" -> updateCustomerProfile(user, request);
            case "ADMIN" -> updateAdminProfile(user, request);
            case "MODERATOR" -> updateModeratorProfile(user, request);
            default -> throw AuthException.badRequest("Unsupported role for profile update");
        }

        return mapToProfile(user);
    }

    private void createRoleProfile(User user, RegisterRequest request) {
        switch (user.getRole().name()) {
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
    String fullName = resolveFullName(user);

    System.out.println("==> LOGIN PROFILE USER: " + user.getEmail());
    System.out.println("==> LOGIN PROFILE ROLE: " + user.getRole());
    System.out.println("==> LOGIN PROFILE FULL NAME: " + fullName);

    UserProfileResponse.UserProfileResponseBuilder builder =
            UserProfileResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .fullName(fullName)
                    .accountStatus(user.getAccountStatus())
                    .createdAt(user.getCreatedAt());

    customerProfileRepository.findByUser_Id(user.getId())
            .ifPresent(profile -> {
                builder.studentCode(profile.getStudentCode());
                builder.schoolName(profile.getSchoolName());
            });

    adminProfileRepository.findByUser_Id(user.getId())
            .ifPresent(profile -> builder.accessLevel(profile.getAccessLevel()));

    moderatorProfileRepository.findByUser_Id(user.getId())
            .ifPresent(profile -> {
                builder.department(profile.getDepartment());
                builder.assignedSubject(profile.getAssignedSubject());
            });

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

    private String generateOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private boolean isBcryptHash(String passwordHash) {
        return passwordHash != null &&
                (passwordHash.startsWith("$2a$")
                        || passwordHash.startsWith("$2b$")
                        || passwordHash.startsWith("$2y$"));
    }

    private String resolveFullName(User user) {
    if (user == null || user.getId() == null) {
        return null;
    }

    String displayName = userRepository.findDisplayNameByUserId(user.getId());

    if (displayName != null && !displayName.isBlank()) {
        return displayName.trim();
    }

    return null;
}
}
package swp391.aistudyhub.service;

import jakarta.servlet.http.HttpServletRequest;
import swp391.aistudyhub.auth.dto.request.*;
import swp391.aistudyhub.auth.dto.response.AuthResponse;
import swp391.aistudyhub.auth.dto.response.UserProfileResponse;
import swp391.aistudyhub.security.CustomUserDetails;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request, HttpServletRequest httpRequest);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(CustomUserDetails user, ChangePasswordRequest request);

    UserProfileResponse getProfile(CustomUserDetails user);

    UserProfileResponse updateProfile(CustomUserDetails user, UpdateProfileRequest request);
}

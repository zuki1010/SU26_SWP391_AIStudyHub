package swp391.aistudyhub.service;

import jakarta.servlet.http.HttpServletRequest;
import swp391.aistudyhub.dto.request.ChangePasswordRequest;
import swp391.aistudyhub.dto.request.ForgotPasswordRequest;
import swp391.aistudyhub.dto.request.LoginRequest;
import swp391.aistudyhub.dto.request.RefreshTokenRequest;
import swp391.aistudyhub.dto.request.RegisterRequest;
import swp391.aistudyhub.dto.request.ResetPasswordRequest;
import swp391.aistudyhub.dto.request.UpdateProfileRequest;
import swp391.aistudyhub.dto.response.AuthResponse;
import swp391.aistudyhub.dto.response.UserProfileResponse;
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

    void verifyEmail(String token);

    void resendVerificationEmail(String email);
}
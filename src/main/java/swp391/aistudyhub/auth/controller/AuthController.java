package swp391.aistudyhub.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import swp391.aistudyhub.config.OpenApiConfig;
import swp391.aistudyhub.auth.dto.request.*;
import swp391.aistudyhub.auth.dto.response.ApiResponse;
import swp391.aistudyhub.auth.dto.response.AuthResponse;
import swp391.aistudyhub.auth.dto.response.UserProfileResponse;
import swp391.aistudyhub.security.CustomUserDetails;
import swp391.aistudyhub.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, password, and profile APIs")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registration successful", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.ok("Login successful", authService.login(request, httpRequest)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed", authService.refreshToken(request)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and invalidate refresh token")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.ok("Logout successful"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset email")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(
                "If the email exists, a password reset link has been sent"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using token from email")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Password reset successful"));
    }

    @PutMapping("/change-password")
    @Operation(summary = "Change password for authenticated user")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(user, request);
        return ResponseEntity.ok(ApiResponse.ok(
                "Password changed successfully. Please login again."));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok("Profile retrieved", authService.getProfile(user)));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update current user profile")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Profile updated", authService.updateProfile(user, request)));
    }
}

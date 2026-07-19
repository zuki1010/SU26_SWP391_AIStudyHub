package swp391.aistudyhub.controller;

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
import swp391.aistudyhub.dto.request.ChangePasswordRequest;
import swp391.aistudyhub.dto.request.ForgotPasswordRequest;
import swp391.aistudyhub.dto.request.LoginRequest;
import swp391.aistudyhub.dto.request.RefreshTokenRequest;
import swp391.aistudyhub.dto.request.RegisterRequest;
import swp391.aistudyhub.dto.request.ResetPasswordRequest;
import swp391.aistudyhub.dto.request.UpdateProfileRequest;
import swp391.aistudyhub.dto.response.ApiResponse;
import swp391.aistudyhub.dto.response.AuthResponse;
import swp391.aistudyhub.dto.response.UserProfileResponse;
import swp391.aistudyhub.security.CustomUserDetails;
import swp391.aistudyhub.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, password, and profile APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản mới")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthResponse response = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registration successful", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập bằng email và mật khẩu")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        AuthResponse response = authService.login(request, httpRequest);

        return ResponseEntity.ok(
                ApiResponse.ok("Login successful", response)
        );
    }

    @PostMapping("/refresh")
    @Operation(summary = "Làm mới access token bằng refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        AuthResponse response = authService.refreshToken(request);

        return ResponseEntity.ok(
                ApiResponse.ok("Token refreshed", response)
        );
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất và hủy refresh token")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        authService.logout(request);

        return ResponseEntity.ok(
                ApiResponse.ok("Logout successful")
        );
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Gửi email đặt lại mật khẩu")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        authService.forgotPassword(request);

        return ResponseEntity.ok(
                ApiResponse.ok("If the email exists, a password reset link has been sent")
        );
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Đặt lại mật khẩu bằng token trong email")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        authService.resetPassword(request);

        return ResponseEntity.ok(
                ApiResponse.ok("Password reset successful")
        );
    }

    @PutMapping("/change-password")
    @Operation(summary = "Đổi mật khẩu của user đang đăng nhập")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(user, request);

        return ResponseEntity.ok(
                ApiResponse.ok("Password changed successfully. Please login again.")
        );
    }

    @GetMapping("/me")
    @Operation(summary = "Lấy thông tin user đang đăng nhập")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        UserProfileResponse response = authService.getProfile(user);

        return ResponseEntity.ok(
                ApiResponse.ok("Profile retrieved", response)
        );
    }

    @PutMapping("/profile")
    @Operation(summary = "Cập nhật profile user đang đăng nhập")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserProfileResponse response = authService.updateProfile(user, request);

        return ResponseEntity.ok(
                ApiResponse.ok("Profile updated", response)
        );
    }
}
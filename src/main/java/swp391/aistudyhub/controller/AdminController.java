package swp391.aistudyhub.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp391.aistudyhub.config.OpenApiConfig;
import swp391.aistudyhub.dto.response.AdminChatResponseDTO;
import swp391.aistudyhub.dto.response.AdminDocumentResponseDTO;
import swp391.aistudyhub.dto.response.AdminStorageResponseDTO;
import swp391.aistudyhub.dto.response.UserAccountResponseDTO;
import swp391.aistudyhub.enums.AccountStatus;
import swp391.aistudyhub.enums.UserRole;
import swp391.aistudyhub.service.AdminService;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "Admin APIs for accounts, documents, chats, and storage")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
@PreAuthorize("hasAnyAuthority('ADMIN','ROLE_ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    @Operation(summary = "Lấy danh sách tài khoản người dùng")
    public ResponseEntity<Page<UserAccountResponseDTO>> getAllUsers(
            @RequestParam(required = false) String key,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(adminService.getAllCustomer(key, page, size));
    }

    @PutMapping("/users/{id}/status")
    @Operation(summary = "Cập nhật trạng thái tài khoản")
    public ResponseEntity<UserAccountResponseDTO> updateUserStatus(
            @PathVariable("id") UUID userId,
            @RequestParam AccountStatus status
    ) {
        return ResponseEntity.ok(adminService.updateUserStatus(userId, status));
    }

    @PutMapping("/users/{id}/role")
    @Operation(summary = "Cập nhật role tài khoản")
    public ResponseEntity<UserAccountResponseDTO> updateUserRole(
            @PathVariable("id") UUID userId,
            @RequestParam UserRole role
    ) {
        return ResponseEntity.ok(adminService.updateUserRole(userId, role));
    }

    @GetMapping("/documents")
    @Operation(summary = "Lấy danh sách tài liệu toàn hệ thống")
    public ResponseEntity<Page<AdminDocumentResponseDTO>> getAllDocuments(
            @RequestParam(required = false) String key,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean isPublic,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                adminService.getAllDocument(key, status, isPublic, page, size)
        );
    }

    @GetMapping("/chats")
    @Operation(summary = "Lấy danh sách tin nhắn chat của người dùng")
    public ResponseEntity<Page<AdminChatResponseDTO>> getAllChat(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(adminService.getAllChat(page, size));
    }

    @GetMapping("/storages")
    @Operation(summary = "Lấy danh sách dung lượng cloud storage của người dùng")
    public ResponseEntity<Page<AdminStorageResponseDTO>> getAllStorageUsage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(adminService.getAllStorage(page, size));
    }
}
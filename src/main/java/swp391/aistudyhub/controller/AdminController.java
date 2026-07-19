package swp391.aistudyhub.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp391.aistudyhub.dto.projection.DocumentResponse;
import swp391.aistudyhub.dto.projection.UserAccountResponse;
import swp391.aistudyhub.enums.AccountStatus;
import swp391.aistudyhub.enums.StatusPublicDoc;
import swp391.aistudyhub.enums.UserRole;
import swp391.aistudyhub.service.AdminService;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin("*")
@Tag(name = "Admin Dashboard", description = "View User Account, View Document List")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/account")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<Page<UserAccountResponse>> getAllUser(
            @RequestParam(required = false) String key,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok().body(adminService.getAllCustomer(key, page, size));
    }

    @PutMapping("/account/status/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable("id") UUID userId,
            @RequestParam AccountStatus status
    ) {
        return ResponseEntity.ok().body(adminService.updateUserStatus(userId, status));
    }

    @PutMapping("/account/role/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<?> updateUserRole(
            @PathVariable("id") UUID userId,
            @RequestParam UserRole role
    ) {
        return ResponseEntity.ok(adminService.updateUserRole(userId, role));
    }

    @GetMapping("/document")
    @PreAuthorize("hasAnyAuthority('MODERATOR', 'ROLE_MODERATOR', 'ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<Page<DocumentResponse>> getAllDocument(
            @RequestParam(required = false) StatusPublicDoc status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok().body(adminService.getAllDocument(page, size, status));
    }

    @GetMapping("/chat")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<?> getAllChatToDay(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(adminService.getAllChat(page, size));
    }

    @GetMapping("/storage")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<?> getAllStorageUsage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(adminService.getAllStorage(page, size));
    }

    @PutMapping("/config-storage")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<?> configureTotalStorageQuota() {
        return ResponseEntity.ok(null);
    }

    @PutMapping("/config-aitoken")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<?> configureChatToken() {
        return ResponseEntity.ok(null);
    }

    @PutMapping("/config-file-size")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<?> configureMaxFileSize() {
        return ResponseEntity.ok(null);
    }

    @PutMapping("/config-file-type")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<?> configureAvailableFileType() {
        return ResponseEntity.ok(null);
    }
}
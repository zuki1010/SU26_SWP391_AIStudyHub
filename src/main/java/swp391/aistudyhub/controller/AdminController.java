package swp391.aistudyhub.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp391.aistudyhub.config.OpenApiConfig;
import swp391.aistudyhub.dto.projection.UserAccountResponse;
import swp391.aistudyhub.dto.request.ApprovePublicRequestDTO;
import swp391.aistudyhub.dto.request.MemberConfigDTO;
import swp391.aistudyhub.dto.request.SystemConfigDTO;
import swp391.aistudyhub.enums.AccountStatus;
import swp391.aistudyhub.enums.StatusPublicDoc;
import swp391.aistudyhub.enums.UserRole;
import swp391.aistudyhub.service.AdminService;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin("*")
@Tag(name = "Admin Dashboard", description = "View User Account, View Document List")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
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
        return ResponseEntity.ok(adminService.getAllCustomer(key, page, size));
    }

    @PutMapping("/account/status/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable("id") UUID userId,
            @RequestParam AccountStatus status
    ) {
        return ResponseEntity.ok(adminService.updateUserStatus(userId, status));
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
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'MODERATOR', 'ROLE_MODERATOR')")
    public ResponseEntity<?> getAllDocument(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) StatusPublicDoc status
    ) {
        return ResponseEntity.ok(adminService.getAllDocument(page, size, status));
    }

    @GetMapping("/approve/documents")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'MODERATOR', 'ROLE_MODERATOR')")
    public ResponseEntity<?> getAllDocumentPending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                adminService.getAllDocument(page, size, StatusPublicDoc.PENDING)
        );
    }

    @PutMapping("/approve/documents/check")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'MODERATOR', 'ROLE_MODERATOR')")
    public ResponseEntity<?> approvePublicDocument(@RequestBody ApprovePublicRequestDTO dto) {
        adminService.approvePublicDocument(dto);
        return ResponseEntity.ok("Duyệt tài liệu thành công.");
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

    @GetMapping("/all/system-config")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<?> getSystemConfig() {
        return ResponseEntity.ok(adminService.getSystemConfig());
    }

    @GetMapping("/all/subscription-config")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<?> getSubscriptionConfig() {
        return ResponseEntity.ok(adminService.getSubscriptionConfig());
    }

    @PutMapping("/config")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<?> updateConfig(@RequestBody SystemConfigDTO dto) {
        adminService.systemConfig(dto);
        return ResponseEntity.ok("Lưu cấu hình thành công.");
    }

    @PutMapping("/config-member")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<?> memberConfig(@RequestBody MemberConfigDTO dto) {
        adminService.memberConfig(dto);
        return ResponseEntity.ok("Lưu cấu hình Premium thành công.");
    }

    @PutMapping("/config-member/price")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<?> updatePriceMember(@RequestBody BigDecimal price) {
        adminService.updatePriceMember(price);
        return ResponseEntity.ok("Cập nhật giá Premium thành công.");
    }
}
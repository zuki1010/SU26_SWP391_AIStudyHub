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
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/account")
    public ResponseEntity<Page<UserAccountResponse>> getAllUser(@RequestParam(required = false) String key,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok().body(adminService.getAllCustomer(key, page, size));
    }

    @PutMapping("/account/status/{id}")
    public ResponseEntity<?> updateUserStatus(@PathVariable("id") UUID userId,
                                              @RequestParam AccountStatus status) {
        return ResponseEntity.ok().body(adminService.updateUserStatus(userId, status));
    }

    @GetMapping("/document")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<?> getAllDocument(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam StatusPublicDoc status
    ) {
        return ResponseEntity.ok().body(adminService.getAllDocument(page, size, status));
    }

    @PutMapping("/account/role/{id}")
    public ResponseEntity<?> updateUserRole(@PathVariable("id") UUID userId,
                                            @RequestParam UserRole role) {
        return ResponseEntity.ok(adminService.updateUserRole(userId, role));
    }

    @GetMapping("/chat")
    public ResponseEntity<?> getAllChatToDay(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.getAllChat(page, size));
    }

    @GetMapping("/storage")
    public ResponseEntity<?> getAllStorageUsage(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.getAllStorage(page, size));
    }

    @PutMapping("/config")
    public ResponseEntity<?> updateConfig(@RequestBody SystemConfigDTO dto) {
        adminService.systemConfig(dto);
        return ResponseEntity.ok("Save Successfully!");
    }

    @PutMapping("/config-member")
    public ResponseEntity<?> memberConfig(@RequestBody MemberConfigDTO dto) {
        adminService.memberConfig(dto);
        return ResponseEntity.ok("Save Successfully!");
    }

    @PutMapping("/config-member/price")
    public ResponseEntity<?> updatePriceMember(@RequestBody BigDecimal price) {
        adminService.updatePriceMember(price);
        return ResponseEntity.ok("Save Successfully!");
    }

    @PutMapping("/approve-public")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<?> approvePublicDocument(@RequestBody ApprovePublicRequestDTO dto) {
        adminService.approvePublicDocument(dto);
        return ResponseEntity.ok("Approve successfully!");
    }
}

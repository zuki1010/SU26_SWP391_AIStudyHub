package swp391.aistudyhub.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp391.aistudyhub.dto.response.UserAccountResponseDTO;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.enums.AccountStatus;
import swp391.aistudyhub.service.AdminService;
import swp391.aistudyhub.service.UserService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin("*")
@Tag(name = "Admin Dashboard", description = "View User Account")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private AdminService adminService;

    @GetMapping("/account")
    public ResponseEntity<Page<List<String>>> getAllUser(@RequestParam(required = false) String key,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 UserAccountResponseDTO dto) {
        List<String> = user
        return ResponseEntity.ok().body(adminService.getAllCustomer(key,page,size));
    }

    @PutMapping("/account/{id}")
    public ResponseEntity<?> updateUserStatus(@PathVariable("id") UUID userId,
                                                @RequestParam AccountStatus status) {
        return ResponseEntity.ok().body(adminService.updateUserStatus(userId, status));
    }

}

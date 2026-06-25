package swp391.aistudyhub.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.aistudyhub.dto.response.CloudStorageUsageResponseDTO; // Đã khớp package response DTO của bạn
import swp391.aistudyhub.service.CloudStorageService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/storage")
@Tag(name = "Cloud Storage Management", description = "Các API quản lý dung lượng bộ nhớ đám mây")
public class CloudStorageController {

    @Autowired
    private CloudStorageService cloudStorageService;

    @GetMapping("/usage")
    @Operation(summary = "Xem thông tin dung lượng bộ nhớ đã sử dụng ")
    public ResponseEntity<?> getStorageUsage(@RequestHeader("X-User-Id") UUID userId) {
        try {
            CloudStorageUsageResponseDTO response = cloudStorageService.getCloudStorageUsage(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
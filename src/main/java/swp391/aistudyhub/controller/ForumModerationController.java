package swp391.aistudyhub.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import swp391.aistudyhub.config.OpenApiConfig;
import swp391.aistudyhub.dto.request.HandleReportRequest;
import swp391.aistudyhub.dto.response.ApiResponse;
import swp391.aistudyhub.dto.response.ReportResponse;
import swp391.aistudyhub.enums.ReportStatus;
import swp391.aistudyhub.security.CustomUserDetails;
import swp391.aistudyhub.service.ForumModerationService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/forum/moderation")
@CrossOrigin(origins = "*")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
@PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
public class ForumModerationController {

    @Autowired
    private ForumModerationService moderationService;

    private UUID currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails u) {
            return u.getId();
        }
        return null;
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable UUID postId) {
        moderationService.deletePostAsModerator(postId);
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa bài đăng."));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable UUID commentId) {
        moderationService.deleteCommentAsModerator(commentId);
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa bình luận."));
    }

    @PutMapping("/posts/{postId}/pin")
    public ResponseEntity<ApiResponse<Void>> pinPost(
            @PathVariable UUID postId,
            @RequestParam(value = "pinned", defaultValue = "true") boolean pinned) {
        moderationService.setPinned(postId, pinned);
        return ResponseEntity.ok(ApiResponse.ok(pinned ? "Đã ghim bài đăng." : "Đã bỏ ghim bài đăng."));
    }

    @GetMapping("/reports")
    public ResponseEntity<Page<ReportResponse>> getReports(
            @RequestParam(value = "status", required = false) ReportStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(moderationService.getReports(
                status, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @PutMapping("/reports/{reportId}")
    public ResponseEntity<ApiResponse<Void>> handleReport(
            @PathVariable UUID reportId,
            @Valid @RequestBody HandleReportRequest request) {
        moderationService.handleReport(currentUserId(), reportId, request.getStatus(), request.isHideContent());
        return ResponseEntity.ok(ApiResponse.ok("Đã xử lý báo cáo."));
    }
}

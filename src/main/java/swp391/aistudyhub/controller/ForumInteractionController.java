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
import swp391.aistudyhub.dto.request.CreateReportRequest;
import swp391.aistudyhub.dto.response.ApiResponse;
import swp391.aistudyhub.dto.response.LikeToggleResponse;
import swp391.aistudyhub.dto.response.PostSummaryResponse;
import swp391.aistudyhub.enums.TargetType;
import swp391.aistudyhub.security.CustomUserDetails;
import swp391.aistudyhub.service.ForumInteractionService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/forum")
@CrossOrigin(origins = "*")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
@PreAuthorize("hasAnyRole('CUSTOMER','MODERATOR','ADMIN')")
public class ForumInteractionController {

    @Autowired
    private ForumInteractionService interactionService;

    private UUID currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails u) {
            return u.getId();
        }
        return null;
    }

    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<ApiResponse<LikeToggleResponse>> togglePostLike(
            @PathVariable UUID postId) {
        LikeToggleResponse result = interactionService.toggleLike(currentUserId(), TargetType.POST, postId);
        return ResponseEntity.ok(ApiResponse.ok(result.isLiked() ? "Đã thích." : "Đã bỏ thích.", result));
    }

    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<ApiResponse<LikeToggleResponse>> toggleCommentLike(
            @PathVariable UUID commentId) {
        LikeToggleResponse result = interactionService.toggleLike(currentUserId(), TargetType.COMMENT, commentId);
        return ResponseEntity.ok(ApiResponse.ok(result.isLiked() ? "Đã thích." : "Đã bỏ thích.", result));
    }

    @PostMapping("/posts/{postId}/bookmark")
    public ResponseEntity<ApiResponse<Boolean>> toggleBookmark(
            @PathVariable UUID postId) {
        boolean bookmarked = interactionService.toggleBookmark(currentUserId(), postId);
        return ResponseEntity.ok(ApiResponse.ok(
                bookmarked ? "Đã lưu bài đăng." : "Đã bỏ lưu bài đăng.", bookmarked));
    }

    @GetMapping("/bookmarks")
    public ResponseEntity<Page<PostSummaryResponse>> getMyBookmarks(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(interactionService.getMyBookmarks(
                currentUserId(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @PostMapping("/reports")
    public ResponseEntity<ApiResponse<Void>> reportContent(
            @Valid @RequestBody CreateReportRequest request) {
        interactionService.reportContent(currentUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Đã gửi báo cáo. Cảm ơn bạn!"));
    }
}

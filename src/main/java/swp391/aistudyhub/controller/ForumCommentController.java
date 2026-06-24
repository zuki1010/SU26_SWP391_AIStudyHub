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
import swp391.aistudyhub.dto.request.CreateCommentRequest;
import swp391.aistudyhub.dto.request.UpdateCommentRequest;
import swp391.aistudyhub.dto.response.ApiResponse;
import swp391.aistudyhub.dto.response.CommentResponse;
import swp391.aistudyhub.security.CustomUserDetails;
import swp391.aistudyhub.service.ForumCommentService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/forum")
@CrossOrigin(origins = "*")
public class ForumCommentController {

    @Autowired
    private ForumCommentService commentService;

    private UUID currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails u) {
            return u.getId();
        }
        return null;
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<Page<CommentResponse>> getComments(
            @PathVariable UUID postId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        Page<CommentResponse> result = commentService.getComments(
                postId, currentUserId(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/posts/{postId}/comments")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    @PreAuthorize("hasAnyRole('CUSTOMER','MODERATOR','ADMIN')")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable UUID postId,
            @Valid @RequestBody CreateCommentRequest request) {
        CommentResponse response = commentService.addComment(currentUserId(), postId, request);
        return ResponseEntity.ok(ApiResponse.ok("Thêm bình luận thành công.", response));
    }

    @PutMapping("/comments/{commentId}")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    @PreAuthorize("hasAnyRole('CUSTOMER','MODERATOR','ADMIN')")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateCommentRequest request) {
        CommentResponse response = commentService.updateComment(currentUserId(), commentId, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật bình luận thành công.", response));
    }

    @DeleteMapping("/comments/{commentId}")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    @PreAuthorize("hasAnyRole('CUSTOMER','MODERATOR','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable UUID commentId) {
        commentService.deleteComment(currentUserId(), commentId);
        return ResponseEntity.ok(ApiResponse.ok("Xóa bình luận thành công."));
    }
}

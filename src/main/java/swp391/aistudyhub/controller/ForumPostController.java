package swp391.aistudyhub.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import swp391.aistudyhub.config.OpenApiConfig;
import swp391.aistudyhub.dto.request.CreatePostRequest;
import swp391.aistudyhub.dto.request.UpdatePostRequest;
import swp391.aistudyhub.dto.response.ApiResponse;
import swp391.aistudyhub.dto.response.PostDetailResponse;
import swp391.aistudyhub.dto.response.PostSummaryResponse;
import swp391.aistudyhub.security.CustomUserDetails;
import swp391.aistudyhub.service.ForumPostService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/forum/posts")
@CrossOrigin(origins = "*")
public class ForumPostController {

    @Autowired
    private ForumPostService postService;

    private UUID currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails u) {
            return u.getId();
        }
        return null;
    }

    // ---------- Public read endpoints (Guest allowed) ----------

    @GetMapping
    public ResponseEntity<Page<PostSummaryResponse>> listPosts(
            @RequestParam(value = "categoryId", required = false) UUID categoryId,
            @RequestParam(value = "sort", defaultValue = "newest") String sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(postService.listPosts(categoryId, sort, PageRequest.of(page, size)));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<PostSummaryResponse>> searchPosts(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(postService.searchPosts(keyword, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDetailResponse> getPostDetail(
            @PathVariable("id") UUID postId) {
        return ResponseEntity.ok(postService.getPostDetail(postId, currentUserId()));
    }

    // ---------- Authenticated write endpoints ----------

    @PostMapping
    @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    @PreAuthorize("hasAnyRole('CUSTOMER','MODERATOR','ADMIN')")
    public ResponseEntity<ApiResponse<PostDetailResponse>> createPost(
            @Valid @RequestBody CreatePostRequest request) {
        PostDetailResponse response = postService.createPost(currentUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Tạo bài đăng thành công.", response));
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    @PreAuthorize("hasAnyRole('CUSTOMER','MODERATOR','ADMIN')")
    public ResponseEntity<ApiResponse<PostDetailResponse>> updatePost(
            @PathVariable("id") UUID postId,
            @Valid @RequestBody UpdatePostRequest request) {
        PostDetailResponse response = postService.updatePost(currentUserId(), postId, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật bài đăng thành công.", response));
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    @PreAuthorize("hasAnyRole('CUSTOMER','MODERATOR','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable("id") UUID postId) {
        postService.deletePost(currentUserId(), postId);
        return ResponseEntity.ok(ApiResponse.ok("Xóa bài đăng thành công."));
    }
}

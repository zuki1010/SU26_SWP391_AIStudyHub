package swp391.aistudyhub.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.aistudyhub.config.OpenApiConfig;
import swp391.aistudyhub.dto.request.ForumPostRequestDTO;
import swp391.aistudyhub.dto.response.ForumPostResponseDTO;
import swp391.aistudyhub.service.ForumPostService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/forum/posts")
@CrossOrigin(origins = "*")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class ForumController {

    @Autowired
    private ForumPostService forumPostService;

    @PostMapping
    @Operation(summary = "Tạo bài viết mới trên diễn đàn (user lấy từ token)")
    public ResponseEntity<?> createPost(@RequestBody ForumPostRequestDTO requestDTO) {
        try {
            ForumPostResponseDTO response = forumPostService.createPost(requestDTO);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/all")
    @Operation(summary = "Lấy danh sách tất cả bài viết trên diễn đàn")
    public ResponseEntity<List<ForumPostResponseDTO>> getAllPosts() {
        return ResponseEntity.ok(forumPostService.getAllPosts());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết một bài viết theo id")
    public ResponseEntity<?> getPostById(@PathVariable("id") UUID postId) {
        try {
            ForumPostResponseDTO response = forumPostService.getPostById(postId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật bài viết (phiên bản cũ được lưu vào revisions)")
    public ResponseEntity<?> updatePost(
            @PathVariable("id") UUID postId,
            @RequestBody ForumPostRequestDTO requestDTO) {
        try {
            ForumPostResponseDTO response = forumPostService.updatePost(postId, requestDTO);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/toggle-visibility")
    @Operation(summary = "Chuyển đổi hiển thị bài viết PUBLIC <-> PRIVATE")
    public ResponseEntity<?> toggleVisibility(@PathVariable("id") UUID postId) {
        try {
            ForumPostResponseDTO response = forumPostService.toggleVisibility(postId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

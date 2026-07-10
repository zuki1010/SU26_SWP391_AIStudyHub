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
}

package swp391.aistudyhub.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.aistudyhub.config.OpenApiConfig;
import swp391.aistudyhub.dto.request.CommentRequestDTO;
import swp391.aistudyhub.dto.response.CommentResponseDTO;
import swp391.aistudyhub.service.CommentService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/comments")
@CrossOrigin(origins = "*")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class CommentController {

    @Autowired
    private CommentService commentService;

    @PostMapping
    @Operation(summary = "Tạo bình luận cho tài liệu (user lấy từ token)")
    public ResponseEntity<?> createComment(@RequestBody CommentRequestDTO requestDTO) {
        try {
            CommentResponseDTO response = commentService.createComment(requestDTO);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/document/{documentId}")
    @Operation(summary = "Xem bình luận của tài liệu (chỉ hiện khi tài liệu đã được duyệt public)")
    public ResponseEntity<List<CommentResponseDTO>> getCommentsByDocument(
            @PathVariable("documentId") UUID documentId) {
        return ResponseEntity.ok(commentService.getCommentsByDocument(documentId));
    }
}

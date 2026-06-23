package swp391.aistudyhub.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import swp391.aistudyhub.config.OpenApiConfig;
import swp391.aistudyhub.dto.request.AttachDocumentRequest;
import swp391.aistudyhub.dto.response.ApiResponse;
import swp391.aistudyhub.dto.response.AttachedDocumentResponse;
import swp391.aistudyhub.security.CustomUserDetails;
import swp391.aistudyhub.service.ForumDocumentService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/forum")
@CrossOrigin(origins = "*")
public class ForumDocumentController {

    @Autowired
    private ForumDocumentService forumDocumentService;

    private UUID currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails u) {
            return u.getId();
        }
        return null;
    }

    @PostMapping("/posts/{postId}/documents")
    @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    @PreAuthorize("hasAnyRole('CUSTOMER','MODERATOR','ADMIN')")
    public ResponseEntity<ApiResponse<List<AttachedDocumentResponse>>> attachDocuments(
            @PathVariable UUID postId,
            @Valid @RequestBody AttachDocumentRequest request) {
        List<AttachedDocumentResponse> result = forumDocumentService.attachDocuments(currentUserId(), postId, request);
        return ResponseEntity.ok(ApiResponse.ok("Đã đính kèm và chia sẻ tài liệu.", result));
    }

    @GetMapping("/posts/{postId}/documents")
    public ResponseEntity<List<AttachedDocumentResponse>> getPostDocuments(@PathVariable UUID postId) {
        return ResponseEntity.ok(forumDocumentService.getPostDocuments(postId));
    }

    @GetMapping("/comments/{commentId}/documents")
    public ResponseEntity<List<AttachedDocumentResponse>> getCommentDocuments(@PathVariable UUID commentId) {
        return ResponseEntity.ok(forumDocumentService.getCommentDocuments(commentId));
    }

    @GetMapping("/documents/{documentId}/preview")
    public ResponseEntity<Resource> previewSharedDocument(@PathVariable UUID documentId) {
        AttachedDocumentResponse meta = forumDocumentService.getSharedDocumentMeta(documentId);
        Resource resource = forumDocumentService.downloadSharedDocument(documentId);

        MediaType mediaType = resolveMediaType(meta.getFileType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + meta.getDocumentName() + "\"")
                .body(resource);
    }

    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<Resource> downloadSharedDocument(@PathVariable UUID documentId) {
        AttachedDocumentResponse meta = forumDocumentService.getSharedDocumentMeta(documentId);
        Resource resource = forumDocumentService.downloadSharedDocument(documentId);

        String fileName = meta.getDocumentName()
                + (meta.getFileType() != null ? "." + meta.getFileType() : "");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }

    private MediaType resolveMediaType(String fileType) {
        if (fileType == null) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        return switch (fileType.toLowerCase()) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "png" -> MediaType.IMAGE_PNG;
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}

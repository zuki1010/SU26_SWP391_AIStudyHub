package swp391.aistudyhub.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import swp391.aistudyhub.config.OpenApiConfig;
import swp391.aistudyhub.dto.request.DocumentRequestDTO;
import swp391.aistudyhub.dto.request.DocumentTogglePublicRequestDTO;
import swp391.aistudyhub.dto.response.DocumentResponseDTO;
import swp391.aistudyhub.entity.Document;
import swp391.aistudyhub.enums.FileType;
import swp391.aistudyhub.security.CustomUserDetails;
import swp391.aistudyhub.service.CloudStorageService;
import swp391.aistudyhub.service.DocumentChunkService;
import swp391.aistudyhub.service.DocumentService;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentChunkService documentChunkService;

    @Autowired
    private CloudStorageService cloudStorageService;

    @GetMapping("/public")
    public ResponseEntity<List<DocumentResponseDTO>> getPublicDocuments() {
        return ResponseEntity.ok(documentService.getPublicDocuments());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tải tài liệu từ máy tính lên hệ thống")
    public ResponseEntity<?> createDocument(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestPart("file") MultipartFile file,
            @RequestParam("description") String description,
            @RequestParam(value = "textContent", required = false) String textContent
    ) {
        try {
            UUID userId = currentUser.getId();

            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("Vui lòng chọn file để upload!");
            }

            if (description == null || description.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("Vui lòng cung cấp mô tả cho tài liệu trước khi upload!");
            }

            String originalName = file.getOriginalFilename();

            if (originalName == null || !originalName.contains(".")) {
                return ResponseEntity.badRequest().body("File không có định dạng hợp lệ!");
            }

            String extension = originalName
                    .substring(originalName.lastIndexOf(".") + 1)
                    .toLowerCase(Locale.ROOT);

            FileType fileType;

            try {
                fileType = FileType.valueOf(extension);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Định dạng file không được hỗ trợ: " + extension);
            }

            String fileUrl = cloudStorageService.uploadFile(file);

            DocumentRequestDTO requestDTO = new DocumentRequestDTO();
            requestDTO.setDocumentName(originalName);
            requestDTO.setFileSize(file.getSize());
            requestDTO.setFileType(fileType);
            requestDTO.setDescription(description.trim());
            requestDTO.setTextContent(
                    textContent != null && !textContent.trim().isEmpty()
                            ? textContent.trim()
                            : description.trim()
            );
            requestDTO.setPreviewUrl(fileUrl);
            requestDTO.setDownloadUrl(fileUrl);

            DocumentResponseDTO response = documentService.createDocument(userId, requestDTO);

            Document docEntity = new Document();
            docEntity.setId(response.getDocumentId());
            documentChunkService.chunkAndEmbedDocument(docEntity, requestDTO.getTextContent());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllMyDocuments(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(documentService.getAllDocumentsByUserId(currentUser.getId()));
    }

    @GetMapping("/search")
    public ResponseEntity<List<DocumentResponseDTO>> searchDocuments(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type
    ) {
        List<DocumentResponseDTO> results = documentService.searchAndFilterDocuments(
                currentUser.getId(),
                name,
                type
        );

        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDocumentById(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable("id") UUID documentId
    ) {
        try {
            DocumentResponseDTO response = documentService.getDocumentDetail(
                    documentId,
                    currentUser.getId()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDocumentName(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable("id") UUID documentId,
            @RequestParam("newName") String newName
    ) {
        try {
            DocumentResponseDTO response = documentService.updateDocumentName(
                    documentId,
                    currentUser.getId(),
                    newName
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable("id") UUID documentId
    ) {
        try {
            documentService.deleteDocument(documentId, currentUser.getId());

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "message", "Xóa thành công tài liệu và giải phóng bộ nhớ!"
                    )
            );
        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", e.getMessage()
                    )
            );
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadDocument(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable("id") UUID documentId
    ) {
        try {
            Resource fileResource = documentService.downloadDocumentFile(
                    documentId,
                    currentUser.getId()
            );

            DocumentResponseDTO detail = documentService.getDocumentDetail(
                    documentId,
                    currentUser.getId()
            );

            String fileName = buildFileName(detail);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName + "\""
                    )
                    .body(fileResource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/preview-file")
    public ResponseEntity<?> previewDocumentFile(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable("id") UUID documentId
    ) {
        try {
            Resource fileResource = documentService.downloadDocumentFile(
                    documentId,
                    currentUser.getId()
            );

            DocumentResponseDTO detail = documentService.getDocumentDetail(
                    documentId,
                    currentUser.getId()
            );

            MediaType mediaType = resolveMediaType(detail.getFileType());
            String fileName = buildFileName(detail);

            ContentDisposition contentDisposition = ContentDisposition.builder("inline")
                    .filename(fileName, StandardCharsets.UTF_8)
                    .build();

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                    .body(fileResource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{documentId}/toggle-public")
    public ResponseEntity<?> toggleDocumentPublic(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable("documentId") UUID documentId,
            @RequestBody DocumentTogglePublicRequestDTO requestDTO
    ) {
        try {
            DocumentResponseDTO response = documentService.toggleDocumentPublicStatus(
                    currentUser.getId(),
                    documentId,
                    requestDTO.getIsPublic()
            );

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    private MediaType resolveMediaType(FileType fileType) {
        if (fileType == null) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        return switch (fileType) {
            case pdf -> MediaType.APPLICATION_PDF;
            case png -> MediaType.IMAGE_PNG;
            case jpg, jpeg -> MediaType.IMAGE_JPEG;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    private String buildFileName(DocumentResponseDTO detail) {
        String fileName = detail.getDocumentName();

        if (fileName == null || fileName.isBlank()) {
            fileName = detail.getDocumentId().toString();
        }

        if (detail.getFileType() == null) {
            return fileName;
        }

        String extension = "." + detail.getFileType().name().toLowerCase(Locale.ROOT);

        if (!fileName.toLowerCase(Locale.ROOT).endsWith(extension)) {
            fileName += extension;
        }

        return fileName;
    }
}
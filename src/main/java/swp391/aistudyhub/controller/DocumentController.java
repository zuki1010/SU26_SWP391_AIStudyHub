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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import swp391.aistudyhub.config.OpenApiConfig;
import swp391.aistudyhub.dto.request.DocumentRequestDTO;
import swp391.aistudyhub.dto.request.DocumentTogglePublicRequestDTO;
import swp391.aistudyhub.dto.request.DocumentUpdateRequestDTO;
import swp391.aistudyhub.dto.response.DocumentResponseDTO;
import swp391.aistudyhub.enums.FileType;
import swp391.aistudyhub.enums.RequestPublicDoc;
import swp391.aistudyhub.enums.StatusPublicDoc;
import swp391.aistudyhub.service.CloudStorageService;
import swp391.aistudyhub.service.DocumentService;
import swp391.aistudyhub.service.DocumentShareService;

import java.nio.charset.StandardCharsets;
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
    private CloudStorageService cloudStorageService;

    @Autowired
    private DocumentShareService documentShareService;

    @GetMapping("/public")
    @Operation(summary = "Lấy danh sách tài liệu cộng đồng đã được public")
    public ResponseEntity<?> getPublicDocuments() {
        return ResponseEntity.ok(documentService.getPublicDocuments());
    }

    @GetMapping("/all")
    @Operation(summary = "Lấy tài liệu của người dùng hiện tại")
    public ResponseEntity<?> getAllMyDocuments() {
        return ResponseEntity.ok(documentService.getAllDocumentsByUser());
    }

    @GetMapping("/search")
    @Operation(summary = "Tìm kiếm tài liệu có thể truy cập theo tên file hoặc danh mục")
    public ResponseEntity<?> searchDocuments(
            @RequestParam(value = "name", required = false) String searchText
    ) {
        return ResponseEntity.ok(documentService.searchDocumentsByFilter(searchText));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tải tài liệu từ máy tính lên hệ thống")
    public ResponseEntity<?> createDocument(
            @RequestPart("file") MultipartFile file,
            @RequestParam("description") String description,
            @RequestParam("categoryId") UUID categoryId
    ) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("Vui lòng chọn file để upload!");
            }

            if (description == null || description.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("Vui lòng cung cấp mô tả cho tài liệu trước khi upload!");
            }

            if (categoryId == null) {
                return ResponseEntity.badRequest().body("Vui lòng chọn môn học hợp lệ!");
            }

            String originalName = file.getOriginalFilename();

            if (originalName == null || !originalName.contains(".")) {
                return ResponseEntity.badRequest().body("File không có định dạng hợp lệ!");
            }

            String extension = originalName
                    .substring(originalName.lastIndexOf(".") + 1)
                    .toLowerCase(Locale.ROOT)
                    .trim();

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
            requestDTO.setStatus(StatusPublicDoc.DEFAULT);
            requestDTO.setDescription(description.trim());
            requestDTO.setTextContent(description.trim());
            requestDTO.setPreviewUrl(fileUrl);
            requestDTO.setDownloadUrl(fileUrl);
            requestDTO.setCategoryId(categoryId);

            DocumentResponseDTO response = documentService.createDocument(requestDTO);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết tài liệu")
    public ResponseEntity<?> getDocumentById(@PathVariable("id") UUID documentId) {
        try {
            return ResponseEntity.ok(documentService.getDocumentDetail(documentId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin tài liệu")
    public ResponseEntity<?> updateDocument(
            @PathVariable("id") UUID documentId,
            @RequestBody(required = false) DocumentUpdateRequestDTO requestDTO,
            @RequestParam(value = "newName", required = false) String newName
    ) {
        try {
            if (requestDTO == null) {
                requestDTO = new DocumentUpdateRequestDTO();
            }

            if (newName != null && !newName.trim().isEmpty()) {
                requestDTO.setDocumentName(newName.trim());
            }

            DocumentResponseDTO response = documentService.updateDocument(documentId, requestDTO);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping(value = "/{id}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Thay thế file của tài liệu đã có")
    public ResponseEntity<?> replaceDocumentFile(
            @PathVariable("id") UUID documentId,
            @RequestPart("file") MultipartFile file
    ) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("Vui lòng chọn file để thay thế!");
            }

            String originalName = file.getOriginalFilename();

            if (originalName == null || !originalName.contains(".")) {
                return ResponseEntity.badRequest().body("File không có định dạng hợp lệ!");
            }

            String extension = originalName
                    .substring(originalName.lastIndexOf(".") + 1)
                    .toLowerCase(Locale.ROOT)
                    .trim();

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
            requestDTO.setPreviewUrl(fileUrl);
            requestDTO.setDownloadUrl(fileUrl);

            DocumentResponseDTO response = documentService.replaceDocumentFile(documentId, requestDTO);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa tài liệu")
    public ResponseEntity<?> deleteDocument(@PathVariable("id") UUID documentId) {
        try {
            documentService.deleteDocument(documentId);

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
    @Operation(summary = "Tải file tài liệu")
    public ResponseEntity<?> downloadDocument(@PathVariable("id") UUID documentId) {
        try {
            Resource fileResource = documentService.downloadDocumentFile(documentId);
            DocumentResponseDTO detail = documentService.getDocumentDetail(documentId);
            String fileName = buildFileName(detail);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(fileResource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/preview-file")
    @Operation(summary = "Xem trước file tài liệu")
    public ResponseEntity<?> previewDocumentFile(@PathVariable("id") UUID documentId) {
        try {
            Resource fileResource = documentService.getFileResourceForPreview(documentId);
            DocumentResponseDTO detail = documentService.getDocumentDetail(documentId);

            MediaType mediaType = resolveMediaType(detail.getFileType());
            String fullFileName = buildFileName(detail);

            ContentDisposition contentDisposition = ContentDisposition.builder("inline")
                    .filename(fullFileName, StandardCharsets.UTF_8)
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
    @Operation(summary = "Gửi yêu cầu public hoặc chuyển tài liệu về private")
    public ResponseEntity<?> toggleDocumentPublic(
            @PathVariable("documentId") UUID documentId,
            @RequestBody DocumentTogglePublicRequestDTO requestDTO
    ) {
        try {
            DocumentResponseDTO response = documentService.toggleDocumentPublicStatus(
                    documentId,
                    Boolean.TRUE.equals(requestDTO.getIsPublic())
            );

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @PutMapping("/{documentId}/review")
    @Operation(summary = "Admin/Moderator duyệt yêu cầu public tài liệu: ACCEPT hoặc DENY")
    public ResponseEntity<?> reviewDocument(
            @PathVariable UUID documentId,
            @RequestParam RequestPublicDoc decision
    ) {
        try {
            return ResponseEntity.ok(documentService.approvePublicRequest(documentId, decision));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/share")
    @Operation(summary = "Chia sẻ quyền truy cập tài liệu cho người dùng khác")
    public ResponseEntity<?> shareDocument(
            @PathVariable("id") UUID documentId,
            @RequestParam("targetUserId") UUID targetUserId,
            @RequestParam(value = "permissionType", required = false, defaultValue = "view") String permissionType
    ) {
        try {
            documentShareService.shareDocumentToUser(documentId, targetUserId, permissionType);

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "message", "Chia sẻ tài liệu thành công!"
                    )
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("success", false, "message", "Lỗi hệ thống: " + e.getMessage())
            );
        }
    }

    @PutMapping("/{id}/share")
    @Operation(summary = "Thay đổi quyền truy cập tài liệu của người được share")
    public ResponseEntity<?> updateSharePermission(
            @PathVariable("id") UUID documentId,
            @RequestParam("targetUserId") UUID targetUserId,
            @RequestParam("permissionType") String permissionType
    ) {
        try {
            documentShareService.updateSharePermission(documentId, targetUserId, permissionType);

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "message", "Cập nhật quyền truy cập thành công sang: " + permissionType
                    )
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("success", false, "message", "Lỗi hệ thống: " + e.getMessage())
            );
        }
    }

    @GetMapping("/get-storage")
    @Operation(summary = "Lấy tổng dung lượng lưu trữ của tài khoản hiện tại")
    public ResponseEntity<?> getTotalQuota() {
        return ResponseEntity.ok(documentService.getTotalQuota());
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
package swp391.aistudyhub.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import swp391.aistudyhub.config.OpenApiConfig;
import swp391.aistudyhub.dto.request.DocumentRequestDTO;
import swp391.aistudyhub.dto.request.DocumentTogglePublicRequestDTO;
import swp391.aistudyhub.dto.request.StartSessionDTO;
import swp391.aistudyhub.dto.response.DocumentResponseDTO;
import swp391.aistudyhub.dto.request.DocumentRequestDTO;
import swp391.aistudyhub.dto.response.DocumentResponseDTO;
import swp391.aistudyhub.entity.Document;
import swp391.aistudyhub.enums.FileType;
import swp391.aistudyhub.service.CloudStorageService;
import swp391.aistudyhub.service.DocumentChunkService;
import swp391.aistudyhub.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp391.aistudyhub.service.DocumentShareService;


import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@CrossOrigin(origins = "*")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
@PreAuthorize("hasRole('CUSTOMER')")
public class    DocumentController {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentChunkService documentChunkService;

    @Autowired
    private CloudStorageService cloudStorageService;

    @Autowired
    private DocumentShareService documentShareService; // 🌟 Tiêm service share vào đây

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tải tài liệu từ máy tính lên hệ thống")
    public ResponseEntity<?> createDocument(
        @RequestPart("file") MultipartFile file,
        @RequestParam("description") String description,
        @RequestParam(value = "textContent", required = false) String textContent,
        @RequestParam("categories") List<String> categoryNames
) {
    try {
        if (description == null || description.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("Vui lòng cung cấp mô tả cho tài liệu trước khi upload!");
        }

            String fileUrl = cloudStorageService.uploadFile(file);

            DocumentRequestDTO requestDTO = new DocumentRequestDTO();

            requestDTO.setDocumentName(file.getOriginalFilename());
            requestDTO.setFileSize(file.getSize());

            String originalName = file.getOriginalFilename();
            String fileType = (originalName != null && originalName.contains("."))
                    ? originalName.substring(originalName.lastIndexOf(".") + 1)
                    : "unknown";
            requestDTO.setFileType(FileType.valueOf(fileType));

            requestDTO.setDescription(description.trim());

            requestDTO.setTextContent(
                    textContent != null && !textContent.trim().isEmpty()
                            ? textContent.trim()
                            : description.trim()
            );

        requestDTO.setPreviewUrl(fileUrl);
        requestDTO.setDownloadUrl(fileUrl);
        requestDTO.setCategoryNames(categoryNames);

            DocumentResponseDTO response = documentService.createDocument(requestDTO);

            Document docEntity = new Document();
            docEntity.setId(response.getDocumentId());
            documentChunkService.chunkAndEmbedDocument(docEntity, requestDTO.getTextContent());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ĐA SỬA: Bỏ /{id} dư thừa trên URL vì bạn đã nhận diện user qua @RequestHeader
    @GetMapping("/all")
    public ResponseEntity<?> getAllMyDocuments() {
        return ResponseEntity.ok(documentService.getAllDocumentsByUser());
    }

    @GetMapping("/public")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<DocumentResponseDTO>> getPublicDocuments() {
        return ResponseEntity.ok(documentService.getPublicDocuments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDocumentById(

            @PathVariable("id") UUID documentId) {
        try {
            DocumentResponseDTO response = documentService.getDocumentDetail(documentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDocumentName(

            @PathVariable("id") UUID documentId,
            @RequestParam("newName") String newName) {
        try {
            DocumentResponseDTO response = documentService.updateDocumentName(documentId, newName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(

            @PathVariable("id") UUID documentId) {
        try {
            documentService.deleteDocument(documentId);

            return ResponseEntity.ok(
                    java.util.Map.of(
                            "success", true,
                            "message", "Xóa thành công tài liệu và giải phóng bộ nhớ!"
                    )
            );
        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.badRequest().body(
                    java.util.Map.of(
                            "success", false,
                            "message", e.getMessage()
                    )
            );
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadDocument(

            @PathVariable("id") UUID documentId) {
        try {
            Resource fileResource = documentService.downloadDocumentFile(documentId);

            // ĐA TỐI ƯU: Lấy thông tin chi tiết để gán đúng tên file gốc và định dạng khi tải về
            DocumentResponseDTO detail = documentService.getDocumentDetail(documentId);
            String fileName = detail.getDocumentName() + "." + detail.getFileType();

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(fileResource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/preview-file")
    public ResponseEntity<?> previewDocumentFile(

            @PathVariable("id") UUID documentId) {
        try {
            Resource fileResource = documentService.getFileResourceForPreview(documentId);
            DocumentResponseDTO detail = documentService.getDocumentDetail(documentId);



            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
            if (detail.getFileType() == FileType.pdf) {
                mediaType = MediaType.APPLICATION_PDF;
            } else if (detail.getFileType() == FileType.png) {
                mediaType = MediaType.IMAGE_PNG;
            } else if (detail.getFileType() == FileType.jpg || detail.getFileType() == FileType.jpeg) {
                mediaType = MediaType.IMAGE_JPEG;
            }

            String fullFileName = detail.getDocumentName();
            if (!fullFileName.toLowerCase().endsWith("." + detail.getFileType())) {
                fullFileName = fullFileName + "." + detail.getFileType();
            }

            org.springframework.http.ContentDisposition contentDisposition = org.springframework.http.ContentDisposition.builder("inline")
                    .filename(fullFileName, java.nio.charset.StandardCharsets.UTF_8)
                    .build();

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                    .body(fileResource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @GetMapping("/search")
    @Operation(summary = "Tìm kiếm tài liệu linh hoạt theo Tên file, Tên danh mục hoặc Lọc theo ID danh mục")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DocumentResponseDTO>> searchDocuments(
            @RequestParam(value = "name", required = false) String searchText
    ) {
        List<DocumentResponseDTO> results = documentService.searchDocumentsByFilter(searchText);
        return ResponseEntity.ok(results);
    }

    @PutMapping("/{documentId}/toggle-public")
    public ResponseEntity<?> toggleDocumentPublic(

            @PathVariable("documentId") UUID documentId,
            @RequestBody DocumentTogglePublicRequestDTO requestDTO) {
        try {
            DocumentResponseDTO response = documentService.toggleDocumentPublicStatus(

                    documentId,
                    requestDTO.getIsPublic()
            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/share")
    @Operation(summary = "Chia sẻ quyền truy cập tài liệu cho người dùng khác")
    public ResponseEntity<?> shareDocument(

            @PathVariable("id") UUID documentId,
            @RequestParam("targetUserId") UUID targetUserId,
            @RequestParam(value = "permissionType", required = false, defaultValue = "view") String permissionType) {
        try {
            documentShareService.shareDocumentToUser(documentId, targetUserId, permissionType);

            return ResponseEntity.ok(
                    java.util.Map.of(
                            "success", true,
                            "message", "Chia sẻ tài liệu thành công!"
                    )
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("success", false, "message", e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    java.util.Map.of("success", false, "message", "Lỗi hệ thống: " + e.getMessage())
            );
        }
    }

    @PutMapping("/{id}/share")
    @Operation(summary = "Thay đổi quyền truy cập tài liệu của người được share (view, download, edit)")
    public ResponseEntity<?> updateSharePermission(

            @PathVariable("id") UUID documentId,
            @RequestParam("targetUserId") UUID targetUserId,
            @RequestParam("permissionType") String permissionType) {
        try {
            documentShareService.updateSharePermission( documentId, targetUserId, permissionType);

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
}
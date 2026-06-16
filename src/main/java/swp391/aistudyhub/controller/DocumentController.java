package swp391.aistudyhub.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import swp391.aistudyhub.config.OpenApiConfig;
import swp391.aistudyhub.dto.DocumentRequestDTO;
import swp391.aistudyhub.dto.DocumentResponseDTO;
import swp391.aistudyhub.entity.Document;
import swp391.aistudyhub.service.CloudStorageService;
import swp391.aistudyhub.service.DocumentChunkService;
import swp391.aistudyhub.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@CrossOrigin(origins = "*")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentChunkService documentChunkService;

    @Autowired
    private CloudStorageService cloudStorageService;

    @PostMapping
    public ResponseEntity<?> createDocument(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody DocumentRequestDTO requestDTO) {
        try {
            DocumentResponseDTO response = documentService.createDocument(userId, requestDTO);

            Document docEntity = new Document();
            docEntity.setId(response.getDocumentId());

            String testLargeText = "Đây là đoạn văn bản dài dùng để kiểm tra tính năng băm nhỏ tài liệu của hệ thống AI Study Hub. "
                    + "Hệ thống sẽ tự động cắt chuỗi này thành các mảnh nhỏ hơn dựa trên cấu hình chuỗi gối đầu (overlap size). "
                    + "Sau đó, mỗi mảnh nhỏ này sẽ được chuyển hóa thành vector và lưu trữ trực tiếp xuống bảng document_chunks trên Supabase. "
                    + "Quá trình này giúp phục vụ cho tính năng tìm kiếm ngữ nghĩa nâng cao và tạo lập giải pháp Chat với tài liệu (RAG) ở các bước tiếp theo.";

            System.out.println(">>> CONTROLLER LOG: Bắt đầu luồng kích hoạt băm chữ thử nghiệm...");
            documentChunkService.chunkAndEmbedDocument(docEntity, testLargeText);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ĐA SỬA: Bỏ /{id} dư thừa trên URL vì bạn đã nhận diện user qua @RequestHeader
    @GetMapping("/all")
    public ResponseEntity<List<DocumentResponseDTO>> getAllMyDocuments(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(documentService.getAllDocumentsByUserId(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDocumentById(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable("id") UUID documentId) {
        try {
            DocumentResponseDTO response = documentService.getDocumentDetail(documentId, userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDocumentName(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable("id") UUID documentId,
            @RequestParam("newName") String newName) {
        try {
            DocumentResponseDTO response = documentService.updateDocumentName(documentId, userId, newName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable("id") UUID documentId,
            @RequestParam("fileSize") Long fileSize) {
        try {
            documentService.deleteDocument(documentId, userId, fileSize);
            return ResponseEntity.ok("Xóa thành công tài liệu và giải phóng bộ nhớ!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadDocument(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable("id") UUID documentId) {
        try {
            Resource fileResource = documentService.downloadDocumentFile(documentId, userId);

            // ĐA TỐI ƯU: Lấy thông tin chi tiết để gán đúng tên file gốc và định dạng khi tải về
            DocumentResponseDTO detail = documentService.getDocumentDetail(documentId, userId);
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
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable("id") UUID documentId) {
        try {
            Resource fileResource = documentService.downloadDocumentFile(documentId, userId);
            DocumentResponseDTO detail = documentService.getDocumentDetail(documentId, userId);

            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
            if (detail.getFileType().equalsIgnoreCase("pdf")) {
                mediaType = MediaType.APPLICATION_PDF;
            } else if (detail.getFileType().equalsIgnoreCase("png")) {
                mediaType = MediaType.IMAGE_PNG;
            } else if (detail.getFileType().equalsIgnoreCase("jpg") || detail.getFileType().equalsIgnoreCase("jpeg")) {
                mediaType = MediaType.IMAGE_JPEG;
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + detail.getDocumentName() + "\"")
                    .body(fileResource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
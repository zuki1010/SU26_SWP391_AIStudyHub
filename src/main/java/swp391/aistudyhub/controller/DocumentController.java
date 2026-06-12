package swp391.aistudyhub.controller;

import swp391.aistudyhub.dto.DocumentRequestDTO;
import swp391.aistudyhub.dto.DocumentResponseDTO;
import swp391.aistudyhub.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @PostMapping
    public ResponseEntity<?> createDocument(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody DocumentRequestDTO requestDTO) {
        try {
            DocumentResponseDTO response = documentService.createDocument(userId, requestDTO);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/all/{id}")
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
}
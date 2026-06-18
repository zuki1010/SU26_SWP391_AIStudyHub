package swp391.aistudyhub.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import swp391.aistudyhub.config.OpenApiConfig;
import swp391.aistudyhub.dto.DocumentRequestDTO;
import swp391.aistudyhub.dto.DocumentResponseDTO;
import swp391.aistudyhub.security.CustomUserDetails;
import swp391.aistudyhub.service.DocumentService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class    DocumentController {

    @Autowired
    private DocumentService documentService;

    @PostMapping
    public ResponseEntity<?> createDocument(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody DocumentRequestDTO requestDTO) {
        try {
            DocumentResponseDTO response =
                    documentService.createDocument(userDetails.getId(), requestDTO);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponseDTO>> getAllMyDocuments(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        System.out.println("DOCUMENT API CALLED");
        System.out.println("USER DETAILS = " + userDetails.getUsername());
        System.out.println("AUTHORITIES = " + userDetails.getAuthorities());

        return ResponseEntity.ok(
                documentService.getAllDocumentsByUserId(userDetails.getId())
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDocumentById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") UUID documentId) {
        try {
            DocumentResponseDTO response =
                    documentService.getDocumentDetail(documentId, userDetails.getId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDocumentName(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") UUID documentId,
            @RequestParam("newName") String newName) {
        try {
            DocumentResponseDTO response =
                    documentService.updateDocumentName(
                            documentId,
                            userDetails.getId(),
                            newName
                    );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") UUID documentId) {
        try {
            documentService.deleteDocument(documentId, userDetails.getId(), 0L);

            return ResponseEntity.ok("Xóa thành công tài liệu!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
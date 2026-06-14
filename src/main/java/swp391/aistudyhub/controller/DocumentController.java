package swp391.aistudyhub.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
<<<<<<< Updated upstream
=======
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
>>>>>>> Stashed changes
import swp391.aistudyhub.config.OpenApiConfig;
import swp391.aistudyhub.dto.DocumentRequestDTO;
import swp391.aistudyhub.dto.DocumentResponseDTO;
import swp391.aistudyhub.security.CustomUserDetails;
import swp391.aistudyhub.service.DocumentService;
<<<<<<< Updated upstream
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
=======
>>>>>>> Stashed changes

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@CrossOrigin(origins = "*")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @PostMapping
    public ResponseEntity<?> createDocument(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody DocumentRequestDTO requestDTO) {
        try {
<<<<<<< Updated upstream
            DocumentResponseDTO response = documentService.createDocument(userDetails.getId(), requestDTO);
=======
            DocumentResponseDTO response =
                    documentService.createDocument(userDetails.getId(), requestDTO);

>>>>>>> Stashed changes
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

<<<<<<< Updated upstream
    @GetMapping("/all/{id}")
    public ResponseEntity<List<DocumentResponseDTO>> getAllMyDocuments(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(documentService.getAllDocumentsByUserId(userDetails.getId()));
=======
    @GetMapping
    public ResponseEntity<List<DocumentResponseDTO>> getAllMyDocuments(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        System.out.println("DOCUMENT API CALLED");
        System.out.println("USER DETAILS = " + userDetails.getUsername());

        return ResponseEntity.ok(
                documentService.getAllDocumentsByUserId(userDetails.getId())
        );
>>>>>>> Stashed changes
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
    @GetMapping
public ResponseEntity<List<DocumentResponseDTO>> getMyDocuments(
        @AuthenticationPrincipal CustomUserDetails userDetails) {

<<<<<<< Updated upstream
    System.out.println("DOCUMENT API CALLED");
    System.out.println("USER DETAILS = " + userDetails);

    return ResponseEntity.ok(
            documentService.getAllDocumentsByUserId(userDetails.getId())
    );
}
=======
    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadDocument(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") UUID documentId) {
        try {
            Resource fileResource =
                    documentService.downloadDocumentFile(documentId, userDetails.getId());

            String fileName = "tai_lieu_hoc_tap";

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
>>>>>>> Stashed changes
}
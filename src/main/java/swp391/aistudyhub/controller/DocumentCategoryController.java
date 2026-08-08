package swp391.aistudyhub.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp391.aistudyhub.config.OpenApiConfig;
import swp391.aistudyhub.dto.request.CreateCategoryDTO;
import swp391.aistudyhub.dto.request.UpdateCategoryDTO;
import swp391.aistudyhub.service.DocumentCategoryService;

import java.util.UUID;

@RestController
@RequestMapping("/api/document-category")
@CrossOrigin("*")
@Tag(name = "Document Category Config", description = "CRUD Search for subjects and semesters")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public class DocumentCategoryController {

    @Autowired
    private DocumentCategoryService documentCategoryService;

    @PostMapping("/add")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ROLE_CUSTOMER', 'MODERATOR', 'ROLE_MODERATOR', 'ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<?> createCategory(@RequestBody CreateCategoryDTO dto) {
        return ResponseEntity.ok(documentCategoryService.addSubject(dto));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ROLE_CUSTOMER', 'MODERATOR', 'ROLE_MODERATOR', 'ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<?> getAllSubject(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "500") int size
    ) {
        return ResponseEntity.ok(documentCategoryService.getAllSubjects(page, size));
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<?> updateCategory(
            @RequestBody UpdateCategoryDTO dto,
            @PathVariable("id") UUID id
    ) {
        return ResponseEntity.ok(documentCategoryService.updateSubject(dto, id));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<?> deleteCategory(@PathVariable("id") UUID id) {
        documentCategoryService.deleteSubject(id);
        return ResponseEntity.ok("Xóa danh mục thành công.");
    }
}
package swp391.aistudyhub.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp391.aistudyhub.config.OpenApiConfig;
import swp391.aistudyhub.dto.request.CreateCategoryDTO;
import swp391.aistudyhub.service.DocumentCategoryService;

import java.util.UUID;

@RestController
@RequestMapping("/api/document-category")
@CrossOrigin("*")
@Tag(name = "Document Category Config", description = "CRUD Search for subjects and semesters")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
@PreAuthorize("hasRole('ADMIN')")
public class DocumentCategoryController {

    @Autowired
    private DocumentCategoryService documentCategoryService;

    @PostMapping("/add")
    public ResponseEntity<?> createCategory(CreateCategoryDTO dto) {
        return ResponseEntity.ok().body(documentCategoryService.addSubject(dto));
    }
    @GetMapping("/all")
    public ResponseEntity<?> getAllSubject(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok().body(documentCategoryService.getAllSubjects(page, size));
    }
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateCategory(@RequestBody CreateCategoryDTO dto,
                                            @PathVariable("id") UUID id) {
        return ResponseEntity.ok().body(documentCategoryService.updateSubject(dto, id));
    }
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable("id") UUID id) {
        documentCategoryService.deleteSubject(id);
        return ResponseEntity.ok().body("Delete Successfully");
    }
}

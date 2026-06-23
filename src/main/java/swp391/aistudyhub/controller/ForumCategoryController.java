package swp391.aistudyhub.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp391.aistudyhub.config.OpenApiConfig;
import swp391.aistudyhub.dto.request.CreateForumCategoryRequest;
import swp391.aistudyhub.dto.response.ApiResponse;
import swp391.aistudyhub.entity.ForumCategory;
import swp391.aistudyhub.exception.AuthException;
import swp391.aistudyhub.repository.ForumCategoryRepository;

import java.util.List;

@RestController
@RequestMapping("/api/v1/forum/categories")
@CrossOrigin(origins = "*")
public class ForumCategoryController {

    @Autowired
    private ForumCategoryRepository categoryRepository;

    // Public: list all categories (Guest allowed)
    @GetMapping
    public ResponseEntity<List<ForumCategory>> listCategories() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }

    @PostMapping
    @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
    @PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")
    public ResponseEntity<ApiResponse<ForumCategory>> createCategory(
            @Valid @RequestBody CreateForumCategoryRequest request) {
        categoryRepository.findByNameIgnoreCase(request.getName()).ifPresent(c -> {
            throw new AuthException("Danh mục đã tồn tại.", HttpStatus.CONFLICT);
        });

        ForumCategory category = new ForumCategory();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        ForumCategory saved = categoryRepository.save(category);
        return ResponseEntity.ok(ApiResponse.ok("Tạo danh mục thành công.", saved));
    }
}

package swp391.aistudyhub.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.aistudyhub.dto.request.CreateCategoryDTO;
import swp391.aistudyhub.dto.request.UpdateCategoryDTO;
import swp391.aistudyhub.entity.DocumentCategory;
import swp391.aistudyhub.repository.DocumentCategoryRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentCategoryService {

    @Autowired
    private DocumentCategoryRepository documentCategoryRepository;

    @Transactional
    public DocumentCategory addSubject(CreateCategoryDTO dto) {
        if (dto == null) {
            throw new RuntimeException("Dữ liệu danh mục không hợp lệ.");
        }

        String categoryName = cleanText(dto.getCategoryName());
        String categoryType = cleanText(dto.getCategoryType());

        if (categoryName == null) {
            throw new RuntimeException("Vui lòng nhập tên danh mục.");
        }

        if (categoryType == null) {
            categoryType = "SUBJECT";
        }

        boolean existed = documentCategoryRepository
                .existsByCategoryNameIgnoreCaseAndCategoryTypeIgnoreCase(categoryName, categoryType);

        if (existed) {
            throw new RuntimeException("Danh mục này đã tồn tại.");
        }

        DocumentCategory documentCategory = new DocumentCategory();
        documentCategory.setCategoryName(categoryName);
        documentCategory.setCategoryType(categoryType.toUpperCase());
        documentCategory.setParentId(dto.getParentId());
        documentCategory.setCreatedAt(Instant.now());

        return documentCategoryRepository.save(documentCategory);
    }

    public Page<DocumentCategory> getAllSubjects(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 100 : Math.min(size, 500);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by("categoryType").ascending().and(Sort.by("categoryName").ascending())
        );

        return documentCategoryRepository.findAll(pageable);
    }

    @Transactional
    public DocumentCategory updateSubject(UpdateCategoryDTO dto, UUID categoryId) {
        if (categoryId == null) {
            throw new RuntimeException("Thiếu mã danh mục.");
        }

        if (dto == null) {
            throw new RuntimeException("Dữ liệu cập nhật không hợp lệ.");
        }

        DocumentCategory documentCategory = documentCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại."));

        String categoryName = cleanText(dto.getCategoryName());
        String categoryType = cleanText(dto.getCategoryType());

        if (categoryName != null) {
            documentCategory.setCategoryName(categoryName);
        }

        if (categoryType != null) {
            documentCategory.setCategoryType(categoryType.toUpperCase());
        }

        documentCategory.setParentId(dto.getParentId());

        return documentCategoryRepository.save(documentCategory);
    }

    @Transactional
    public void deleteSubject(UUID categoryId) {
        if (categoryId == null) {
            throw new RuntimeException("Thiếu mã danh mục.");
        }

        DocumentCategory documentCategory = documentCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại."));

        if(documentCategory.getParentId() == null) {
            List<DocumentCategory> children = documentCategoryRepository.findAllByParentId(categoryId);
            if (!children.isEmpty()) {
                documentCategoryRepository.deleteAll(children);
            }
        }

        documentCategoryRepository.delete(documentCategory);
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }

        String text = value.trim();

        return text.isEmpty() ? null : text;
    }
}
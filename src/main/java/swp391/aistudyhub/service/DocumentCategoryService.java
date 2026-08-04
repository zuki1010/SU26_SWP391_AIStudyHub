package swp391.aistudyhub.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.aistudyhub.dto.request.CreateCategoryDTO;
import swp391.aistudyhub.entity.DocumentCategory;
import swp391.aistudyhub.repository.DocumentCategoryRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentCategoryService {

    @Autowired
    private DocumentCategoryRepository documentCategoryRepository;

    public DocumentCategory addSubject(CreateCategoryDTO dto) {
        DocumentCategory documentCategory = new DocumentCategory();
        documentCategory.setCategoryType(dto.getCategoryType());
        documentCategory.setCategoryName(dto.getCategoryName());

        documentCategory.setParentId(dto.getParentId());
        documentCategory.setCreatedAt(Instant.now());

        documentCategoryRepository.save(documentCategory);
        return documentCategory;
    }

    public Page<DocumentCategory> getAllSubjects(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("categoryType").descending());

        return documentCategoryRepository.findAll(pageable);
    }

    @Transactional
    public DocumentCategory updateSubject(CreateCategoryDTO dto, UUID categoryId) {
        DocumentCategory documentCategory = documentCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("This Category is not exist!"));

        if (dto.getCategoryName() != null) {
            documentCategory.setCategoryName(dto.getCategoryName());
        }
        if (dto.getCategoryType() != null) {
            documentCategory.setCategoryType(dto.getCategoryType());
        }
        if (dto.getParentId() != null) {
            documentCategory.setParentId(dto.getParentId());
        }

        documentCategoryRepository.save(documentCategory);
        return documentCategory;
    }

    @Transactional
    public void deleteSubject(UUID categoryId) {
        DocumentCategory documentCategory = documentCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("This Category is not exist!"));

        documentCategoryRepository.delete(documentCategory);
    }
}

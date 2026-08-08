package swp391.aistudyhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import swp391.aistudyhub.entity.DocumentCategory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentCategoryRepository extends JpaRepository<DocumentCategory, UUID> {

    boolean existsByCategoryNameIgnoreCaseAndCategoryTypeIgnoreCase(
            String categoryName,
            String categoryType
    );

    Optional<DocumentCategory> findByCategoryNameIgnoreCaseAndCategoryTypeIgnoreCase(
            String categoryName,
            String categoryType
    );

    List<DocumentCategory> findByCategoryTypeIgnoreCaseOrderByCategoryNameAsc(String categoryType);

    void deleteAllByParentId(UUID parentId);

    boolean existsByParentId(UUID parentId);

    List<DocumentCategory> findAllByParentId(UUID parentId);
}
package swp391.aistudyhub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.aistudyhub.dto.projection.DocumentResponse;
import swp391.aistudyhub.entity.Document;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID>, JpaSpecificationExecutor<Document> {
    // ĐÃ SỬA: Tìm danh sách Document trực tiếp theo UserId (thay thế cho findByStorage_User_Id)
    List<Document> findByUserId(UUID userId);

    // ĐÃ SỬA: Tìm Document theo ID và UserId (thay thế cho findByIdAndStorage_User_Id)
    Optional<Document> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT COALESCE(SUM(d.fileSize), 0) FROM Document d WHERE d.user.id = :userId")
    long sumFileSizeByUserId(@Param("userId") UUID userId);

    List<Document> findByIsPublicTrueOrderByCreatedAtDesc();

    Page<DocumentResponse> findBy(Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.user.id = :userId OR d.isPublic = true " +
            "OR d.id IN (SELECT ds.document.id FROM DocumentShare ds WHERE ds.sharedWithUser.id = :userId)")
    List<Document> findAccessibleDocuments(@Param("userId") UUID userId);

    // 🌟 ĐÃ TÍCH HỢP: Hàm tìm kiếm nâng cao theo Tên file HOẶC Tên danh mục (Category), hỗ trợ lọc theo ID danh mục
    @Query("SELECT DISTINCT d FROM Document d " +
            "LEFT JOIN d.user u " +
            "LEFT JOIN DocumentShare ds ON ds.document.id = d.id AND ds.sharedWithUser.id = :userId " +
            "LEFT JOIN d.documentCategories dc " +
            "WHERE (u.id = :userId OR d.isPublic = true OR ds IS NOT NULL) " +
            "AND (:searchText IS NULL OR LOWER(d.documentName) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "    OR LOWER(dc.categoryName) LIKE LOWER(CONCAT('%', :searchText, '%'))) " +
            "AND (:categoryId IS NULL OR dc.id = :categoryId)")
    List<Document> searchDocumentsWithCategory(
            @Param("userId") UUID userId,
            @Param("searchText") String searchText,
            @Param("categoryId") UUID categoryId);
}
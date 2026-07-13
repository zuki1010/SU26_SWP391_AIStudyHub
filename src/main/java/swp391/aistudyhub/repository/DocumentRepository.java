package swp391.aistudyhub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.aistudyhub.dto.projection.DocumentResponse;
import swp391.aistudyhub.dto.response.DocumentResponseDTO;
import swp391.aistudyhub.entity.Document;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.enums.SubjectCode;

import java.util.Collection;
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

    List<Document> findByUser(User user);

    @Query("SELECT d FROM Document d WHERE d.user.id = :userId OR d.isPublic = true " +
            "OR d.id IN (SELECT ds.document.id FROM DocumentShare ds WHERE ds.sharedWithUser.id = :userId)")
    List<Document> findAccessibleDocuments(@Param("userId") UUID userId);

    // 🌟 HÀM TÌM KIẾM THÔNG MINH CHO TÀI LIỆU PUBLIC (Gõ mã môn hoặc tên file)
    // 🌟 HÀM SEARCH ĐỈNH CAO: Tìm tài liệu PRIVATE của tôi + tài liệu PUBLIC của toàn trường
    @Query("SELECT d FROM Document d " +
            "WHERE (d.user.id = :userId OR d.isPublic = true) " + // Điều kiện bảo mật quyết định ở đây
            "AND (:searchText IS NULL OR :searchText = '' " +
            "    OR LOWER(d.documentName) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "    OR LOWER(CAST(d.categoryId AS string)) LIKE LOWER(CONCAT('%', :searchText, '%')))")
    List<Document> searchSmartAccessibleDocuments(
            @Param("userId") UUID userId,
            @Param("searchText") String searchText);
}
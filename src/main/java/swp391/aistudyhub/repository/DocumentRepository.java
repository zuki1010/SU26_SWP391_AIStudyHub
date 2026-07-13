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

    @Query("SELECT d FROM Document d WHERE " +
            "(" +
            "   d.user.id = :userId " +             // 1. Tài liệu do chính mình sở hữu
            "   OR d.isPublic = true " +           // 2. Tài liệu công khai (Public)
            "   OR d.id IN (SELECT ds.document.id FROM DocumentShare ds WHERE ds.sharedWithUser.id = :userId)" + // 🔥 3. CHÍNH LÀ ĐÂY: Tài liệu người khác share private cho mình
            ") " +
            "AND (CAST(:searchText AS string) IS NULL OR :searchText = '' " +
            "    OR LOWER(d.documentName) LIKE LOWER(CONCAT('%', :searchText, '%')) " +
            "    OR d.id IN (" +
            "        SELECT dc.document.id FROM DocumentCategory dc " +
            "        WHERE LOWER(dc.categoryName) LIKE LOWER(CONCAT('%', :searchText, '%'))" +
            "    )" +
            ")")
    List<Document> searchSmartAccessibleDocuments(
            @Param("userId") java.util.UUID userId,
            @Param("searchText") String searchText);
}
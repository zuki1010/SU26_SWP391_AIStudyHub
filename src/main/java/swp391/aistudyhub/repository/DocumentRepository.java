package swp391.aistudyhub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.aistudyhub.dto.projection.DocumentResponse;
import swp391.aistudyhub.entity.Document;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.enums.StatusPublicDoc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByUserId(UUID userId);

    Optional<Document> findByIdAndUserId(UUID id, UUID userId);

    List<Document> findByUser(User user);

    List<Document> findByIsPublicTrueOrderByCreatedAtDesc();

    @Query("""
            SELECT COALESCE(SUM(d.fileSize), 0)
            FROM Document d
            WHERE d.user.id = :userId
            """)
    long sumFileSizeByUserId(@Param("userId") UUID userId);

    /*
     * Giữ method cũ để AdminServiceImpl.getAllDocument(page, size) vẫn chạy.
     * Method này gọi lại query native đầy đủ bên dưới.
     */
    default Page<DocumentResponse> findBy(Pageable pageable) {
        return findAllAdminDocuments(pageable);
    }

    /*
     * Giữ method cũ nếu nơi khác còn gọi findByPending(pageable).
     */
    default Page<DocumentResponse> findByPending(Pageable pageable) {
        return findAdminDocumentsByStatus(StatusPublicDoc.PENDING, pageable);
    }

    @Query("""
            SELECT d
            FROM Document d
            WHERE d.user.id = :userId
               OR d.isPublic = true
               OR d.id IN (
                    SELECT ds.document.id
                    FROM DocumentShare ds
                    WHERE ds.sharedWithUser.id = :userId
               )
            ORDER BY d.createdAt DESC
            """)
    List<Document> findAccessibleDocuments(@Param("userId") UUID userId);

    @Query("""
                SELECT d
                FROM Document d
                WHERE
                (
                    d.user.id = :userId
                    OR d.isPublic = true
                    OR d.id IN (
                        SELECT ds.document.id
                        FROM DocumentShare ds
                        WHERE ds.sharedWithUser.id = :userId
                    )
                )
                AND
                (
                    :searchText = ''
                    OR LOWER(d.documentName) LIKE LOWER(CONCAT('%', :searchText, '%'))
                    OR LOWER(d.description) LIKE LOWER(CONCAT('%', :searchText, '%'))
                    OR LOWER(d.category.categoryName) LIKE LOWER(CONCAT('%', :searchText, '%'))
                )
                ORDER BY d.createdAt DESC
            """)
    List<Document> searchSmartAccessibleDocuments(@Param("userId") UUID userId, @Param("searchText") String searchText);

    @Query("""
            SELECT d
            FROM Document d
            WHERE
            (
                :key IS NULL
                OR :key = ''
                OR LOWER(d.documentName) LIKE LOWER(CONCAT('%', :key, '%'))
                OR LOWER(d.description) LIKE LOWER(CONCAT('%', :key, '%'))
                OR LOWER(d.user.email) LIKE LOWER(CONCAT('%', :key, '%'))
            )
            AND
            (
                :status IS NULL
                OR d.status = :status
            )
            AND
            (
                :isPublic IS NULL
                OR d.isPublic = :isPublic
            )
            """)
    Page<Document> searchAdminDocuments(
            @Param("key") String key,
            @Param("status") StatusPublicDoc status,
            @Param("isPublic") Boolean isPublic,
            Pageable pageable
    );

    /*
     * Admin/Moderator: lấy toàn bộ tài liệu kèm tên người gửi.
     */
    @Query(
            value = """
                    SELECT
                        d.document_id AS documentId,
                        d.document_name AS documentName,
                        d.file_type AS fileType,
                        d.file_size AS fileSize,
                        d.created_at AS createdAt,
                        d.is_public AS isPublic,
                        d.status AS status,

                        u.user_id AS userId,
                        u.email AS userEmail,

                        COALESCE(
                            cp.full_name,
                            mp.full_name,
                            ap.full_name,
                            u.email
                        ) AS userFullName,

                        COALESCE(
                            cp.full_name,
                            mp.full_name,
                            ap.full_name,
                            u.email
                        ) AS authorName,

                        COALESCE(
                            cp.full_name,
                            mp.full_name,
                            ap.full_name,
                            u.email
                        ) AS uploaderName,

                        COALESCE(
                            cp.full_name,
                            mp.full_name,
                            ap.full_name,
                            u.email
                        ) AS userName
                    FROM documents d
                    LEFT JOIN users u ON d.user_id = u.user_id
                    LEFT JOIN customer_profiles cp ON cp.user_id = u.user_id
                    LEFT JOIN moderator_profiles mp ON mp.user_id = u.user_id
                    LEFT JOIN admin_profiles ap ON ap.user_id = u.user_id
                    ORDER BY d.created_at DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM documents d
                    """,
            nativeQuery = true
    )
    Page<DocumentResponse> findAllAdminDocuments(Pageable pageable);

    /*
     * Method AdminServiceImpl đang gọi:
     * documentRepository.findAdminDocumentsByStatus(status, pageable)
     *
     * Ta giữ tên method này để không phải sửa AdminServiceImpl.
     * Nhưng bên trong chuyển enum sang String để native SQL chạy ổn.
     */
    default Page<DocumentResponse> findAdminDocumentsByStatus(
            StatusPublicDoc status,
            Pageable pageable
    ) {
        if (status == null) {
            return findAllAdminDocuments(pageable);
        }

        return findAdminDocumentsByStatusValue(status.name(), pageable);
    }

    /*
     * Query thật dùng String status.
     */
    @Query(
            value = """
                    SELECT
                        d.document_id AS documentId,
                        d.document_name AS documentName,
                        d.file_type AS fileType,
                        d.file_size AS fileSize,
                        d.created_at AS createdAt,
                        d.is_public AS isPublic,
                        d.status AS status,

                        u.user_id AS userId,
                        u.email AS userEmail,

                        COALESCE(
                            cp.full_name,
                            mp.full_name,
                            ap.full_name,
                            u.email
                        ) AS userFullName,

                        COALESCE(
                            cp.full_name,
                            mp.full_name,
                            ap.full_name,
                            u.email
                        ) AS authorName,

                        COALESCE(
                            cp.full_name,
                            mp.full_name,
                            ap.full_name,
                            u.email
                        ) AS uploaderName,

                        COALESCE(
                            cp.full_name,
                            mp.full_name,
                            ap.full_name,
                            u.email
                        ) AS userName
                    FROM documents d
                    LEFT JOIN users u ON d.user_id = u.user_id
                    LEFT JOIN customer_profiles cp ON cp.user_id = u.user_id
                    LEFT JOIN moderator_profiles mp ON mp.user_id = u.user_id
                    LEFT JOIN admin_profiles ap ON ap.user_id = u.user_id
                    WHERE d.status = :status
                    ORDER BY d.created_at DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM documents d
                    WHERE d.status = :status
                    """,
            nativeQuery = true
    )
    Page<DocumentResponse> findAdminDocumentsByStatusValue(
            @Param("status") String status,
            Pageable pageable
    );
}
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
    Double sumFileSizeByUserId(@Param("userId") UUID userId);

    default Page<DocumentResponse> findBy(Pageable pageable) {
        return findAllAdminDocuments(pageable);
    }

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
    List<Document> searchSmartAccessibleDocuments(
            @Param("userId") UUID userId,
            @Param("searchText") String searchText
    );

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
                OR LOWER(d.category.categoryName) LIKE LOWER(CONCAT('%', :key, '%'))
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

                        dc.category_id AS categoryId,
                        dc.category_name AS categoryName,
                        dc.category_type AS categoryType,
                        dc.parent_id AS parentCategoryId,

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
                    LEFT JOIN document_categories dc ON dc.category_id = d.category_id
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

    default Page<DocumentResponse> findAdminDocumentsByStatus(
            StatusPublicDoc status,
            Pageable pageable
    ) {
        if (status == null) {
            return findAllAdminDocuments(pageable);
        }

        return findAdminDocumentsByStatusValue(status.name(), pageable);
    }

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

                        dc.category_id AS categoryId,
                        dc.category_name AS categoryName,
                        dc.category_type AS categoryType,
                        dc.parent_id AS parentCategoryId,

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
                    LEFT JOIN document_categories dc ON dc.category_id = d.category_id
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
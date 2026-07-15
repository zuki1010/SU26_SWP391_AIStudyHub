package swp391.aistudyhub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import swp391.aistudyhub.entity.Document;
import swp391.aistudyhub.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByUserId(UUID userId);

    Optional<Document> findByIdAndUserId(UUID id, UUID userId);

    List<Document> findByUser(User user);

    List<Document> findByIsPublicTrueOrderByCreatedAtDesc();

    @Query("SELECT COALESCE(SUM(d.fileSize), 0) FROM Document d WHERE d.user.id = :userId")
    long sumFileSizeByUserId(UUID userId);

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
            """)
    List<Document> findAccessibleDocuments(UUID userId);

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
            OR d.id IN (
                SELECT dc.document.id
                FROM DocumentCategory dc
                WHERE LOWER(dc.categoryName) LIKE LOWER(CONCAT('%', :searchText, '%'))
            )
        )
        """)
List<Document> searchSmartAccessibleDocuments(UUID userId, String searchText);

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
                OR :status = ''
                OR UPPER(d.status) = UPPER(:status)
            )
            AND
            (
                :isPublic IS NULL
                OR d.isPublic = :isPublic
            )
            """)
    Page<Document> searchAdminDocuments(
            String key,
            String status,
            Boolean isPublic,
            Pageable pageable
    );
}
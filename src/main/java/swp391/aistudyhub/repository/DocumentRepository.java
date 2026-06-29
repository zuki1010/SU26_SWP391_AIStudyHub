package swp391.aistudyhub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.aistudyhub.dto.projection.DocumentResponse;
import swp391.aistudyhub.entity.Document;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    // ĐÃ SỬA: Tìm danh sách Document trực tiếp theo UserId (thay thế cho findByStorage_User_Id)
    List<Document> findByUserId(UUID userId);

    // ĐÃ SỬA: Tìm Document theo ID và UserId (thay thế cho findByIdAndStorage_User_Id)
    Optional<Document> findByIdAndUserId(UUID id, UUID userId);

    Page<DocumentResponse> findBy(Pageable pageable);
}
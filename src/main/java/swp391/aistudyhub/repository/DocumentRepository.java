package swp391.aistudyhub.repository;

import swp391.aistudyhub.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.aistudyhub.entity.User;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    // Tìm tài liệu dựa vào thuộc tính user.id của Entity Document
    List<Document> findByUserId(UUID userId);

    List<Document> findAllByUser(User user);

}
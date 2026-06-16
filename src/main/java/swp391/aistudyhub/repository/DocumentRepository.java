package swp391.aistudyhub.repository;

import swp391.aistudyhub.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.aistudyhub.entity.Document;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByUser_Id(UUID userId);

    Optional<Document> findByIdAndUser_Id(UUID documentId, UUID userId);
}
package swp391.aistudyhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.aistudyhub.entity.CloudStorage;
import swp391.aistudyhub.entity.Document;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByStorage_User_Id(UUID userId);

    Optional<Document> findByIdAndStorage_User_Id(UUID id, UUID userId);

    List<Document> findAllByStorage(CloudStorage storage);
}
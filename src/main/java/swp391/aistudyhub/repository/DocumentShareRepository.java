package swp391.aistudyhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.aistudyhub.entity.DocumentShare;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentShareRepository extends JpaRepository<DocumentShare, UUID> {

    boolean existsByDocument_IdAndSharedWithUser_Id(UUID documentId, UUID userId);

    Optional<DocumentShare> findByDocument_IdAndSharedWithUser_Id(UUID documentId, UUID userId);
}
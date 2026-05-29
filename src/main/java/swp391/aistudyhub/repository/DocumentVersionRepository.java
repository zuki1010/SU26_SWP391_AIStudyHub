package swp391.aistudyhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.aistudyhub.entity.DocumentVersion;

import java.util.UUID;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, UUID> {
}
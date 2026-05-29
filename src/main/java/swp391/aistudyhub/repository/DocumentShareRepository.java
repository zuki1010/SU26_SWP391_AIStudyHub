package swp391.aistudyhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.aistudyhub.entity.DocumentShare;

import java.util.UUID;

public interface DocumentShareRepository extends JpaRepository<DocumentShare, UUID> {
}
package swp391.aistudyhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.aistudyhub.entity.Document;

import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
}
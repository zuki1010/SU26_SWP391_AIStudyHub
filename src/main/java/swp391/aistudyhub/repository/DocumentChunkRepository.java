package swp391.aistudyhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.aistudyhub.entity.DocumentChunk;

import java.util.UUID;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {
}
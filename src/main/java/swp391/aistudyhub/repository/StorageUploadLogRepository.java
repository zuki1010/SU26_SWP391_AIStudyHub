package swp391.aistudyhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.aistudyhub.entity.StorageUploadLog;

import java.util.UUID;

public interface StorageUploadLogRepository extends JpaRepository<StorageUploadLog, UUID> {
}
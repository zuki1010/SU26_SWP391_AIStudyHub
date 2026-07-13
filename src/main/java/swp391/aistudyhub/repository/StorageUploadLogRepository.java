package swp391.aistudyhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.aistudyhub.entity.StorageUploadLog;

import java.util.UUID;

@Repository
public interface StorageUploadLogRepository extends JpaRepository<StorageUploadLog, UUID> {
}
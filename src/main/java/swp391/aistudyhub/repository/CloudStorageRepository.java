package swp391.aistudyhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.aistudyhub.entity.CloudStorage;

import java.util.UUID;

public interface CloudStorageRepository extends JpaRepository<CloudStorage, UUID> {
}
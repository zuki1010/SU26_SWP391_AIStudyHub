package swp391.aistudyhub.repository;

import swp391.aistudyhub.entity.CloudStorage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CloudStorageRepository extends JpaRepository<CloudStorage, UUID> {
    Optional<CloudStorage> findByUserId(UUID userId);
}
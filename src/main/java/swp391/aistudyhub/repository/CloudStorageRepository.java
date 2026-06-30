package swp391.aistudyhub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import swp391.aistudyhub.dto.projection.DocumentResponse;
import swp391.aistudyhub.dto.projection.StorageUsageResponse;
import swp391.aistudyhub.entity.CloudStorage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CloudStorageRepository extends JpaRepository<CloudStorage, UUID> {
    Optional<CloudStorage> findByUser_Id(UUID userId);

    Page<StorageUsageResponse> findBy(Pageable pageable);
}
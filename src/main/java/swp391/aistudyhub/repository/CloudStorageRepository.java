package swp391.aistudyhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.aistudyhub.entity.CloudStorage;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CloudStorageRepository extends JpaRepository<CloudStorage, UUID> {

    Optional<CloudStorage> findByUser_Id(UUID userId);
}
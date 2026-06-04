package swp391.aistudyhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.aistudyhub.entity.ModeratorProfile;

import java.util.Optional;
import java.util.UUID;

public interface ModeratorProfileRepository extends JpaRepository<ModeratorProfile, UUID> {

    Optional<ModeratorProfile> findByUser_Id(UUID userId);
}

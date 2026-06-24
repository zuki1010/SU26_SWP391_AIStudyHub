package swp391.aistudyhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.aistudyhub.entity.ForumLike;
import swp391.aistudyhub.enums.TargetType;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ForumLikeRepository extends JpaRepository<ForumLike, UUID> {

    Optional<ForumLike> findByUser_IdAndTargetTypeAndTargetId(UUID userId, TargetType targetType, UUID targetId);

    boolean existsByUser_IdAndTargetTypeAndTargetId(UUID userId, TargetType targetType, UUID targetId);
}

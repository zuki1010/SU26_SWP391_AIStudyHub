package swp391.aistudyhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import swp391.aistudyhub.entity.ForumPostRevision;

import java.util.List;
import java.util.UUID;

public interface ForumPostRevisionRepository extends JpaRepository<ForumPostRevision, UUID> {
    List<ForumPostRevision> findByPostIdOrderByCreatedAtDesc(UUID postId);
    void deleteByPostId(UUID postId);
}

package swp391.aistudyhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.aistudyhub.entity.ForumMention;

import java.util.UUID;

@Repository
public interface ForumMentionRepository extends JpaRepository<ForumMention, UUID> {
}

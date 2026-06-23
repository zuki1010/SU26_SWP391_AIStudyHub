package swp391.aistudyhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.aistudyhub.entity.ForumTag;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ForumTagRepository extends JpaRepository<ForumTag, UUID> {
    Optional<ForumTag> findByNameIgnoreCase(String name);
}

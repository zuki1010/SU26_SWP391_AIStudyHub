package swp391.aistudyhub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.aistudyhub.entity.ForumBookmark;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ForumBookmarkRepository extends JpaRepository<ForumBookmark, UUID> {

    Optional<ForumBookmark> findByUser_IdAndPost_Id(UUID userId, UUID postId);

    boolean existsByUser_IdAndPost_Id(UUID userId, UUID postId);

    Page<ForumBookmark> findByUser_Id(UUID userId, Pageable pageable);
}

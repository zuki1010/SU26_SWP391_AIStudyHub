package swp391.aistudyhub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.aistudyhub.entity.ForumComment;

import java.util.List;
import java.util.UUID;

@Repository
public interface ForumCommentRepository extends JpaRepository<ForumComment, UUID> {

    /**
     * Root-level comments of a post (parent is null), paginated.
     */
    Page<ForumComment> findByPost_IdAndParentIsNull(UUID postId, Pageable pageable);

    /**
     * Replies under a given root comment, oldest first.
     */
    List<ForumComment> findByParent_IdOrderByCreatedAtAsc(UUID parentId);

    @Modifying
    @Query("UPDATE ForumComment c SET c.likeCount = c.likeCount + :delta WHERE c.id = :commentId")
    void adjustLikeCount(@Param("commentId") UUID commentId, @Param("delta") long delta);
}

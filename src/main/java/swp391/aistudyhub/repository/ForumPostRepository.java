package swp391.aistudyhub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swp391.aistudyhub.entity.ForumPost;
import swp391.aistudyhub.enums.PostStatus;

import java.util.UUID;

@Repository
public interface ForumPostRepository extends JpaRepository<ForumPost, UUID> {

    Page<ForumPost> findByStatusNot(PostStatus status, Pageable pageable);

    Page<ForumPost> findByCategoryIdAndStatusNot(UUID categoryId, PostStatus status, Pageable pageable);

    @Query("SELECT p FROM ForumPost p WHERE p.status <> :excludedStatus AND " +
            "(LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<ForumPost> searchByKeyword(@Param("keyword") String keyword,
                                    @Param("excludedStatus") PostStatus excludedStatus,
                                    Pageable pageable);

    @Modifying
    @Query("UPDATE ForumPost p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
    void incrementViewCount(@Param("postId") UUID postId);

    @Modifying
    @Query("UPDATE ForumPost p SET p.commentCount = p.commentCount + :delta WHERE p.id = :postId")
    void adjustCommentCount(@Param("postId") UUID postId, @Param("delta") long delta);

    @Modifying
    @Query("UPDATE ForumPost p SET p.likeCount = p.likeCount + :delta WHERE p.id = :postId")
    void adjustLikeCount(@Param("postId") UUID postId, @Param("delta") long delta);
}

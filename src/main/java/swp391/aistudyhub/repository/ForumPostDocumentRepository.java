package swp391.aistudyhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.aistudyhub.entity.ForumPostDocument;

import java.util.List;
import java.util.UUID;

@Repository
public interface ForumPostDocumentRepository extends JpaRepository<ForumPostDocument, UUID> {

    List<ForumPostDocument> findByPost_IdAndCommentIsNull(UUID postId);

    List<ForumPostDocument> findByComment_Id(UUID commentId);
}

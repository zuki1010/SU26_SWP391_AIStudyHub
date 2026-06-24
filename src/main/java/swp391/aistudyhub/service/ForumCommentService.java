package swp391.aistudyhub.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import swp391.aistudyhub.dto.request.CreateCommentRequest;
import swp391.aistudyhub.dto.request.UpdateCommentRequest;
import swp391.aistudyhub.dto.response.CommentResponse;

import java.util.UUID;

public interface ForumCommentService {

    CommentResponse addComment(UUID userId, UUID postId, CreateCommentRequest request);

    CommentResponse updateComment(UUID userId, UUID commentId, UpdateCommentRequest request);

    void deleteComment(UUID userId, UUID commentId);

    /**
     * Root comments of a post with their replies nested one level deep.
     * viewerId may be null for guests.
     */
    Page<CommentResponse> getComments(UUID postId, UUID viewerId, Pageable pageable);
}

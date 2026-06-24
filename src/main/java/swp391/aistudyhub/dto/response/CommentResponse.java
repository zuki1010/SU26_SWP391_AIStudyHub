package swp391.aistudyhub.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class CommentResponse {
    private final UUID commentId;
    private final UUID postId;
    private final UUID authorId;
    private final String authorName;
    private final UUID parentId;
    private final UUID quotedCommentId;
    private final String quotedContent;
    private final String content;
    private final long likeCount;
    private final boolean likedByMe;
    private final Instant createdAt;
    private final Instant updatedAt;
    /**
     * Direct replies (only populated for root comments).
     */
    private final List<CommentResponse> replies;
}

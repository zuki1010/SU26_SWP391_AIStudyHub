package swp391.aistudyhub.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full post representation used on the detail screen.
 */
@Getter
@Builder
public class PostDetailResponse {
    private final UUID postId;
    private final String title;
    private final String content;
    private final UUID authorId;
    private final String authorName;
    private final UUID categoryId;
    private final String categoryName;
    private final List<String> tags;
    private final String status;
    private final long likeCount;
    private final long commentCount;
    private final long viewCount;
    private final boolean likedByMe;
    private final boolean bookmarkedByMe;
    private final List<AttachedDocumentResponse> attachedDocuments;
    private final Instant createdAt;
    private final Instant updatedAt;
}

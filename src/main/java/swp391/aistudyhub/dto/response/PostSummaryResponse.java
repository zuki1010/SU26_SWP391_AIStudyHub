package swp391.aistudyhub.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Lightweight post representation used in listings / search results.
 */
@Getter
@Builder
public class PostSummaryResponse {
    private final UUID postId;
    private final String title;
    private final UUID authorId;
    private final String authorName;
    private final String categoryName;
    private final List<String> tags;
    private final String status;
    private final long likeCount;
    private final long commentCount;
    private final long viewCount;
    private final Instant createdAt;
}

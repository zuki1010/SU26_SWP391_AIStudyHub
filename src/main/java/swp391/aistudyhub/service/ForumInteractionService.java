package swp391.aistudyhub.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import swp391.aistudyhub.dto.request.CreateReportRequest;
import swp391.aistudyhub.dto.response.LikeToggleResponse;
import swp391.aistudyhub.dto.response.PostSummaryResponse;
import swp391.aistudyhub.enums.TargetType;

import java.util.UUID;

public interface ForumInteractionService {

    /**
     * Toggles a like on a post or comment for the given user and returns the new state.
     */
    LikeToggleResponse toggleLike(UUID userId, TargetType targetType, UUID targetId);

    /**
     * Toggles a bookmark on a post. Returns true if now bookmarked, false if removed.
     */
    boolean toggleBookmark(UUID userId, UUID postId);

    Page<PostSummaryResponse> getMyBookmarks(UUID userId, Pageable pageable);

    void reportContent(UUID userId, CreateReportRequest request);
}

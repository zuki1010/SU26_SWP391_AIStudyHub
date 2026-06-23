package swp391.aistudyhub.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import swp391.aistudyhub.dto.response.ReportResponse;
import swp391.aistudyhub.enums.ReportStatus;

import java.util.UUID;

public interface ForumModerationService {

    /**
     * Deletes any post regardless of owner (moderator/admin action).
     */
    void deletePostAsModerator(UUID postId);

    /**
     * Deletes any comment regardless of owner (moderator/admin action).
     */
    void deleteCommentAsModerator(UUID commentId);

    /**
     * Pins or unpins a post.
     */
    void setPinned(UUID postId, boolean pinned);

    Page<ReportResponse> getReports(ReportStatus status, Pageable pageable);

    /**
     * Resolves a report. When hideContent is true the reported post/comment is hidden/deleted.
     */
    void handleReport(UUID moderatorId, UUID reportId, ReportStatus newStatus, boolean hideContent);
}

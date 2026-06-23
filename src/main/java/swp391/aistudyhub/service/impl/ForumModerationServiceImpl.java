package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.aistudyhub.dto.response.ReportResponse;
import swp391.aistudyhub.entity.ForumComment;
import swp391.aistudyhub.entity.ForumPost;
import swp391.aistudyhub.entity.ForumReport;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.enums.PostStatus;
import swp391.aistudyhub.enums.ReportStatus;
import swp391.aistudyhub.enums.TargetType;
import swp391.aistudyhub.exception.AuthException;
import swp391.aistudyhub.repository.ForumCommentRepository;
import swp391.aistudyhub.repository.ForumPostRepository;
import swp391.aistudyhub.repository.ForumReportRepository;
import swp391.aistudyhub.repository.UserRepository;
import swp391.aistudyhub.service.ForumModerationService;

import java.time.Instant;
import java.util.UUID;

@Service
public class ForumModerationServiceImpl implements ForumModerationService {

    @Autowired
    private ForumPostRepository postRepository;

    @Autowired
    private ForumCommentRepository commentRepository;

    @Autowired
    private ForumReportRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public void deletePostAsModerator(UUID postId) {
        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new AuthException("Không tìm thấy bài đăng.", HttpStatus.NOT_FOUND));
        postRepository.delete(post);
    }

    @Override
    @Transactional
    public void deleteCommentAsModerator(UUID commentId) {
        ForumComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AuthException("Không tìm thấy bình luận.", HttpStatus.NOT_FOUND));
        removeCommentAndAdjustCount(comment);
    }

    @Override
    @Transactional
    public void setPinned(UUID postId, boolean pinned) {
        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new AuthException("Không tìm thấy bài đăng.", HttpStatus.NOT_FOUND));
        // Don't override a hidden post; only toggle between NORMAL and PINNED.
        if (post.getStatus() == PostStatus.HIDDEN) {
            throw new AuthException("Không thể ghim bài đăng đang bị ẩn.", HttpStatus.BAD_REQUEST);
        }
        post.setStatus(pinned ? PostStatus.PINNED : PostStatus.NORMAL);
        postRepository.save(post);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponse> getReports(ReportStatus status, Pageable pageable) {
        Page<ForumReport> reports = (status != null)
                ? reportRepository.findByStatus(status, pageable)
                : reportRepository.findAll(pageable);
        return reports.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public void handleReport(UUID moderatorId, UUID reportId, ReportStatus newStatus, boolean hideContent) {
        User moderator = userRepository.findById(moderatorId)
                .orElseThrow(() -> new AuthException("Không tìm thấy người xử lý.", HttpStatus.NOT_FOUND));
        ForumReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new AuthException("Không tìm thấy báo cáo.", HttpStatus.NOT_FOUND));

        if (hideContent) {
            if (report.getTargetType() == TargetType.POST) {
                postRepository.findById(report.getTargetId()).ifPresent(post -> {
                    post.setStatus(PostStatus.HIDDEN);
                    postRepository.save(post);
                });
            } else {
                commentRepository.findById(report.getTargetId())
                        .ifPresent(this::removeCommentAndAdjustCount);
            }
        }

        report.setStatus(newStatus);
        report.setHandledBy(moderator);
        report.setHandledAt(Instant.now());
        reportRepository.save(report);
    }

    // ---------- helpers ----------

    private void removeCommentAndAdjustCount(ForumComment comment) {
        long removed = 1 + (comment.getParent() == null
                ? commentRepository.findByParent_IdOrderByCreatedAtAsc(comment.getId()).size()
                : 0);
        UUID postId = comment.getPost().getId();
        commentRepository.delete(comment);
        postRepository.adjustCommentCount(postId, -removed);
    }

    private ReportResponse mapToResponse(ForumReport r) {
        return ReportResponse.builder()
                .reportId(r.getId())
                .targetType(r.getTargetType().name())
                .targetId(r.getTargetId())
                .reason(r.getReason())
                .status(r.getStatus().name())
                .reporterId(r.getReporter().getId())
                .reporterEmail(r.getReporter().getEmail())
                .handledById(r.getHandledBy() != null ? r.getHandledBy().getId() : null)
                .handledAt(r.getHandledAt())
                .createdAt(r.getCreatedAt())
                .build();
    }
}

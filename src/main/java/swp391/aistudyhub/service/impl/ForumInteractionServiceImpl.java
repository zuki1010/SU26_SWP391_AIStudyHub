package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.aistudyhub.dto.request.CreateReportRequest;
import swp391.aistudyhub.dto.response.LikeToggleResponse;
import swp391.aistudyhub.dto.response.PostSummaryResponse;
import swp391.aistudyhub.entity.*;
import swp391.aistudyhub.enums.NotificationType;
import swp391.aistudyhub.enums.ReportStatus;
import swp391.aistudyhub.enums.TargetType;
import swp391.aistudyhub.exception.AuthException;
import swp391.aistudyhub.repository.*;
import swp391.aistudyhub.service.ForumInteractionService;
import swp391.aistudyhub.service.NotificationService;

import java.util.UUID;

@Service
public class ForumInteractionServiceImpl implements ForumInteractionService {

    @Autowired
    private ForumLikeRepository likeRepository;

    @Autowired
    private ForumBookmarkRepository bookmarkRepository;

    @Autowired
    private ForumReportRepository reportRepository;

    @Autowired
    private ForumPostRepository postRepository;

    @Autowired
    private ForumCommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Override
    @Transactional
    public LikeToggleResponse toggleLike(UUID userId, TargetType targetType, UUID targetId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("Không tìm thấy người dùng.", HttpStatus.NOT_FOUND));

        // Resolve target & its owner up front (also validates existence).
        User targetOwner;
        if (targetType == TargetType.POST) {
            ForumPost post = postRepository.findById(targetId)
                    .orElseThrow(() -> new AuthException("Không tìm thấy bài đăng.", HttpStatus.NOT_FOUND));
            targetOwner = post.getAuthor();
        } else {
            ForumComment comment = commentRepository.findById(targetId)
                    .orElseThrow(() -> new AuthException("Không tìm thấy bình luận.", HttpStatus.NOT_FOUND));
            targetOwner = comment.getAuthor();
        }

        var existing = likeRepository.findByUser_IdAndTargetTypeAndTargetId(userId, targetType, targetId);

        boolean nowLiked;
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            adjustCount(targetType, targetId, -1);
            nowLiked = false;
        } else {
            ForumLike like = new ForumLike();
            like.setUser(user);
            like.setTargetType(targetType);
            like.setTargetId(targetId);
            likeRepository.save(like);
            adjustCount(targetType, targetId, 1);
            nowLiked = true;

            NotificationType type = targetType == TargetType.POST
                    ? NotificationType.LIKE_ON_POST : NotificationType.LIKE_ON_COMMENT;
            UUID postIdForLink = resolvePostId(targetType, targetId);
            notificationService.notify(targetOwner, user, type,
                    user.getEmail() + " đã thích nội dung của bạn.", postIdForLink);
        }

        long likeCount = currentLikeCount(targetType, targetId);
        return LikeToggleResponse.builder().liked(nowLiked).likeCount(likeCount).build();
    }

    @Override
    @Transactional
    public boolean toggleBookmark(UUID userId, UUID postId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("Không tìm thấy người dùng.", HttpStatus.NOT_FOUND));
        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new AuthException("Không tìm thấy bài đăng.", HttpStatus.NOT_FOUND));

        var existing = bookmarkRepository.findByUser_IdAndPost_Id(userId, postId);
        if (existing.isPresent()) {
            bookmarkRepository.delete(existing.get());
            return false;
        }
        ForumBookmark bookmark = new ForumBookmark();
        bookmark.setUser(user);
        bookmark.setPost(post);
        bookmarkRepository.save(bookmark);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> getMyBookmarks(UUID userId, Pageable pageable) {
        return bookmarkRepository.findByUser_Id(userId, pageable)
                .map(bm -> mapToSummary(bm.getPost()));
    }

    @Override
    @Transactional
    public void reportContent(UUID userId, CreateReportRequest request) {
        User reporter = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("Không tìm thấy người dùng.", HttpStatus.NOT_FOUND));

        // Validate the target exists.
        if (request.getTargetType() == TargetType.POST) {
            postRepository.findById(request.getTargetId())
                    .orElseThrow(() -> new AuthException("Không tìm thấy bài đăng.", HttpStatus.NOT_FOUND));
        } else {
            commentRepository.findById(request.getTargetId())
                    .orElseThrow(() -> new AuthException("Không tìm thấy bình luận.", HttpStatus.NOT_FOUND));
        }

        // Prevent duplicate pending reports from the same user on the same target.
        if (reportRepository.existsByReporter_IdAndTargetIdAndStatus(
                userId, request.getTargetId(), ReportStatus.PENDING)) {
            throw new AuthException("Bạn đã báo cáo nội dung này và đang chờ xử lý.", HttpStatus.CONFLICT);
        }

        ForumReport report = new ForumReport();
        report.setReporter(reporter);
        report.setTargetType(request.getTargetType());
        report.setTargetId(request.getTargetId());
        report.setReason(request.getReason());
        report.setStatus(ReportStatus.PENDING);
        reportRepository.save(report);
    }

    // ---------- helpers ----------

    private void adjustCount(TargetType targetType, UUID targetId, long delta) {
        if (targetType == TargetType.POST) {
            postRepository.adjustLikeCount(targetId, delta);
        } else {
            commentRepository.adjustLikeCount(targetId, delta);
        }
    }

    private long currentLikeCount(TargetType targetType, UUID targetId) {
        if (targetType == TargetType.POST) {
            return postRepository.findById(targetId).map(ForumPost::getLikeCount).orElse(0L);
        }
        return commentRepository.findById(targetId).map(ForumComment::getLikeCount).orElse(0L);
    }

    private UUID resolvePostId(TargetType targetType, UUID targetId) {
        if (targetType == TargetType.POST) {
            return targetId;
        }
        return commentRepository.findById(targetId)
                .map(c -> c.getPost().getId())
                .orElse(null);
    }

    private PostSummaryResponse mapToSummary(ForumPost post) {
        return PostSummaryResponse.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .authorId(post.getAuthor().getId())
                .authorName(post.getAuthor().getCustomerProfile() != null
                        && post.getAuthor().getCustomerProfile().getFullName() != null
                        ? post.getAuthor().getCustomerProfile().getFullName()
                        : post.getAuthor().getEmail())
                .categoryName(post.getCategory() != null ? post.getCategory().getName() : null)
                .tags(post.getTags().stream().map(ForumTag::getName).toList())
                .status(post.getStatus().name())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .viewCount(post.getViewCount())
                .createdAt(post.getCreatedAt())
                .build();
    }
}

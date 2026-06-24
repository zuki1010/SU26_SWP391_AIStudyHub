package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.aistudyhub.dto.request.CreateCommentRequest;
import swp391.aistudyhub.dto.request.UpdateCommentRequest;
import swp391.aistudyhub.dto.response.CommentResponse;
import swp391.aistudyhub.entity.ForumComment;
import swp391.aistudyhub.entity.ForumMention;
import swp391.aistudyhub.entity.ForumPost;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.enums.NotificationType;
import swp391.aistudyhub.exception.AuthException;
import swp391.aistudyhub.repository.ForumCommentRepository;
import swp391.aistudyhub.repository.ForumLikeRepository;
import swp391.aistudyhub.repository.ForumMentionRepository;
import swp391.aistudyhub.repository.ForumPostRepository;
import swp391.aistudyhub.repository.UserRepository;
import swp391.aistudyhub.service.ForumCommentService;
import swp391.aistudyhub.service.NotificationService;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import swp391.aistudyhub.enums.TargetType;

@Service
public class ForumCommentServiceImpl implements ForumCommentService {

    /**
     * Matches @mention tokens where the handle is an email address, e.g. "@john@example.com".
     */
    private static final Pattern MENTION_PATTERN =
            Pattern.compile("@([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})");

    @Autowired
    private ForumCommentRepository commentRepository;

    @Autowired
    private ForumPostRepository postRepository;

    @Autowired
    private ForumMentionRepository mentionRepository;

    @Autowired
    private ForumLikeRepository likeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Override
    @Transactional
    public CommentResponse addComment(UUID userId, UUID postId, CreateCommentRequest request) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("Không tìm thấy người dùng.", HttpStatus.NOT_FOUND));

        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new AuthException("Không tìm thấy bài đăng.", HttpStatus.NOT_FOUND));

        ForumComment comment = new ForumComment();
        comment.setPost(post);
        comment.setAuthor(author);
        comment.setContent(request.getContent());

        // Resolve parent: flatten to 2 levels — a reply always hangs off the ROOT comment.
        ForumComment parentRoot = null;
        if (request.getParentId() != null) {
            ForumComment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new AuthException("Bình luận gốc không tồn tại.", HttpStatus.BAD_REQUEST));
            if (!Objects.equals(parent.getPost().getId(), postId)) {
                throw new AuthException("Bình luận gốc không thuộc bài đăng này.", HttpStatus.BAD_REQUEST);
            }
            parentRoot = (parent.getParent() != null) ? parent.getParent() : parent;
            comment.setParent(parentRoot);
        }

        if (request.getQuotedCommentId() != null) {
            ForumComment quoted = commentRepository.findById(request.getQuotedCommentId())
                    .orElseThrow(() -> new AuthException("Bình luận trích dẫn không tồn tại.", HttpStatus.BAD_REQUEST));
            comment.setQuotedComment(quoted);
        }

        ForumComment saved = commentRepository.save(comment);
        postRepository.adjustCommentCount(postId, 1);

        // --- Notifications ---
        if (parentRoot == null) {
            // Top-level comment -> notify the post author.
            notificationService.notify(post.getAuthor(), author, NotificationType.COMMENT_ON_POST,
                    author.getEmail() + " đã bình luận về bài đăng của bạn.", post.getId());
        } else {
            // Reply -> notify the author of the comment being replied to.
            notificationService.notify(parentRoot.getAuthor(), author, NotificationType.REPLY_ON_COMMENT,
                    author.getEmail() + " đã trả lời bình luận của bạn.", post.getId());
        }

        processMentions(saved, author);

        return mapToResponse(saved, userId, false);
    }

    @Override
    @Transactional
    public CommentResponse updateComment(UUID userId, UUID commentId, UpdateCommentRequest request) {
        ForumComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AuthException("Không tìm thấy bình luận.", HttpStatus.NOT_FOUND));

        if (!Objects.equals(comment.getAuthor().getId(), userId)) {
            throw new AuthException("Bạn không có quyền chỉnh sửa bình luận này.", HttpStatus.FORBIDDEN);
        }

        comment.setContent(request.getContent());
        ForumComment saved = commentRepository.save(comment);
        return mapToResponse(saved, userId, false);
    }

    @Override
    @Transactional
    public void deleteComment(UUID userId, UUID commentId) {
        ForumComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AuthException("Không tìm thấy bình luận.", HttpStatus.NOT_FOUND));

        if (!Objects.equals(comment.getAuthor().getId(), userId)) {
            throw new AuthException("Bạn không có quyền xóa bình luận này.", HttpStatus.FORBIDDEN);
        }

        // A root comment cascades to its replies (orphanRemoval). Adjust the post counter accordingly.
        long removed = 1 + (comment.getParent() == null
                ? commentRepository.findByParent_IdOrderByCreatedAtAsc(comment.getId()).size()
                : 0);

        UUID postId = comment.getPost().getId();
        commentRepository.delete(comment);
        postRepository.adjustCommentCount(postId, -removed);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> getComments(UUID postId, UUID viewerId, Pageable pageable) {
        Page<ForumComment> roots = commentRepository.findByPost_IdAndParentIsNull(postId, pageable);
        return roots.map(root -> {
            CommentResponse base = mapToResponse(root, viewerId, false);
            List<CommentResponse> replies = commentRepository
                    .findByParent_IdOrderByCreatedAtAsc(root.getId())
                    .stream()
                    .map(reply -> mapToResponse(reply, viewerId, false))
                    .toList();
            return rebuildWithReplies(base, replies);
        });
    }

    // ---------- helpers ----------

    private void processMentions(ForumComment comment, User actor) {
        Matcher matcher = MENTION_PATTERN.matcher(comment.getContent());
        Set<UUID> notified = new HashSet<>();
        while (matcher.find()) {
            String email = matcher.group(1);
            userRepository.findByEmailIgnoreCase(email).ifPresent(mentioned -> {
                if (notified.contains(mentioned.getId())) {
                    return;
                }
                notified.add(mentioned.getId());

                ForumMention mention = new ForumMention();
                mention.setMentionedUser(mentioned);
                mention.setComment(comment);
                mention.setPost(comment.getPost());
                mentionRepository.save(mention);

                notificationService.notify(mentioned, actor, NotificationType.MENTION,
                        actor.getEmail() + " đã nhắc đến bạn trong một bình luận.",
                        comment.getPost().getId());
            });
        }
    }

    private CommentResponse mapToResponse(ForumComment comment, UUID viewerId, boolean ignored) {
        boolean liked = viewerId != null && likeRepository
                .existsByUser_IdAndTargetTypeAndTargetId(viewerId, TargetType.COMMENT, comment.getId());

        return CommentResponse.builder()
                .commentId(comment.getId())
                .postId(comment.getPost().getId())
                .authorId(comment.getAuthor().getId())
                .authorName(resolveAuthorName(comment.getAuthor()))
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .quotedCommentId(comment.getQuotedComment() != null ? comment.getQuotedComment().getId() : null)
                .quotedContent(comment.getQuotedComment() != null ? comment.getQuotedComment().getContent() : null)
                .content(comment.getContent())
                .likeCount(comment.getLikeCount())
                .likedByMe(liked)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .replies(null)
                .build();
    }

    private CommentResponse rebuildWithReplies(CommentResponse base, List<CommentResponse> replies) {
        return CommentResponse.builder()
                .commentId(base.getCommentId())
                .postId(base.getPostId())
                .authorId(base.getAuthorId())
                .authorName(base.getAuthorName())
                .parentId(base.getParentId())
                .quotedCommentId(base.getQuotedCommentId())
                .quotedContent(base.getQuotedContent())
                .content(base.getContent())
                .likeCount(base.getLikeCount())
                .likedByMe(base.isLikedByMe())
                .createdAt(base.getCreatedAt())
                .updatedAt(base.getUpdatedAt())
                .replies(replies)
                .build();
    }

    private String resolveAuthorName(User user) {
        if (user.getCustomerProfile() != null && user.getCustomerProfile().getFullName() != null) {
            return user.getCustomerProfile().getFullName();
        }
        return user.getEmail();
    }
}

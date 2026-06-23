package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.aistudyhub.dto.request.CreatePostRequest;
import swp391.aistudyhub.dto.request.UpdatePostRequest;
import swp391.aistudyhub.dto.response.AttachedDocumentResponse;
import swp391.aistudyhub.dto.response.PostDetailResponse;
import swp391.aistudyhub.dto.response.PostSummaryResponse;
import swp391.aistudyhub.entity.*;
import swp391.aistudyhub.enums.PostStatus;
import swp391.aistudyhub.enums.TargetType;
import swp391.aistudyhub.exception.AuthException;
import swp391.aistudyhub.repository.*;
import swp391.aistudyhub.service.ForumPostService;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class ForumPostServiceImpl implements ForumPostService {

    @Autowired
    private ForumPostRepository postRepository;

    @Autowired
    private ForumCategoryRepository categoryRepository;

    @Autowired
    private ForumTagRepository tagRepository;

    @Autowired
    private ForumPostDocumentRepository postDocumentRepository;

    @Autowired
    private ForumLikeRepository likeRepository;

    @Autowired
    private ForumBookmarkRepository bookmarkRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public PostDetailResponse createPost(UUID userId, CreatePostRequest request) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("Không tìm thấy người dùng.", HttpStatus.NOT_FOUND));

        ForumPost post = new ForumPost();
        post.setAuthor(author);
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setCategory(resolveCategory(request.getCategoryId()));
        post.setTags(resolveTags(request.getTags()));
        post.setStatus(PostStatus.NORMAL);

        ForumPost saved = postRepository.save(post);

        attachDocuments(saved, author, request.getDocumentIds());

        return mapToDetail(saved, userId);
    }

    @Override
    @Transactional
    public PostDetailResponse updatePost(UUID userId, UUID postId, UpdatePostRequest request) {
        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new AuthException("Không tìm thấy bài đăng.", HttpStatus.NOT_FOUND));

        if (!Objects.equals(post.getAuthor().getId(), userId)) {
            throw new AuthException("Bạn không có quyền chỉnh sửa bài đăng này.", HttpStatus.FORBIDDEN);
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setCategory(resolveCategory(request.getCategoryId()));
        post.setTags(resolveTags(request.getTags()));

        ForumPost saved = postRepository.save(post);
        return mapToDetail(saved, userId);
    }

    @Override
    @Transactional
    public void deletePost(UUID userId, UUID postId) {
        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new AuthException("Không tìm thấy bài đăng.", HttpStatus.NOT_FOUND));

        if (!Objects.equals(post.getAuthor().getId(), userId)) {
            throw new AuthException("Bạn không có quyền xóa bài đăng này.", HttpStatus.FORBIDDEN);
        }

        postRepository.delete(post);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> listPosts(UUID categoryId, String sort, Pageable pageable) {
        Pageable effective = applySort(sort, pageable);
        Page<ForumPost> page = (categoryId != null)
                ? postRepository.findByCategoryIdAndStatusNot(categoryId, PostStatus.HIDDEN, effective)
                : postRepository.findByStatusNot(PostStatus.HIDDEN, effective);
        return page.map(this::mapToSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> searchPosts(String keyword, Pageable pageable) {
        String safeKeyword = keyword == null ? "" : keyword.trim();
        return postRepository.searchByKeyword(safeKeyword, PostStatus.HIDDEN, pageable)
                .map(this::mapToSummary);
    }

    @Override
    @Transactional
    public PostDetailResponse getPostDetail(UUID postId, UUID viewerId) {
        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new AuthException("Không tìm thấy bài đăng.", HttpStatus.NOT_FOUND));

        if (post.getStatus() == PostStatus.HIDDEN) {
            throw new AuthException("Bài đăng không khả dụng.", HttpStatus.NOT_FOUND);
        }

        // Managed entity: dirty-checking flushes the increment at commit (single UPDATE).
        post.setViewCount(post.getViewCount() + 1);

        return mapToDetail(post, viewerId);
    }

    // ---------- helpers ----------

    private Pageable applySort(String sort, Pageable pageable) {
        Sort sortBy = "hot".equalsIgnoreCase(sort)
                ? Sort.by(Sort.Direction.DESC, "likeCount", "commentCount", "viewCount")
                : Sort.by(Sort.Direction.DESC, "createdAt");
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortBy);
    }

    private ForumCategory resolveCategory(UUID categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AuthException("Danh mục không tồn tại.", HttpStatus.BAD_REQUEST));
    }

    private Set<ForumTag> resolveTags(List<String> tagNames) {
        Set<ForumTag> tags = new HashSet<>();
        if (tagNames == null) {
            return tags;
        }
        for (String raw : tagNames) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String name = raw.trim();
            ForumTag tag = tagRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> {
                        ForumTag created = new ForumTag();
                        created.setName(name);
                        return tagRepository.save(created);
                    });
            tags.add(tag);
        }
        return tags;
    }

    /**
     * Attaches documents owned by the author to the post. Reused by the document-sharing feature.
     */
    private void attachDocuments(ForumPost post, User author, List<UUID> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return;
        }
        for (UUID docId : documentIds) {
            Document document = documentRepository.findByIdAndUserId(docId, author.getId())
                    .orElseThrow(() -> new AuthException(
                            "Tài liệu không tồn tại hoặc không thuộc về bạn: " + docId, HttpStatus.BAD_REQUEST));

            ForumPostDocument link = new ForumPostDocument();
            link.setPost(post);
            link.setDocument(document);
            postDocumentRepository.save(link);
        }
    }

    private PostSummaryResponse mapToSummary(ForumPost post) {
        return PostSummaryResponse.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .authorId(post.getAuthor().getId())
                .authorName(resolveAuthorName(post.getAuthor()))
                .categoryName(post.getCategory() != null ? post.getCategory().getName() : null)
                .tags(post.getTags().stream().map(ForumTag::getName).toList())
                .status(post.getStatus().name())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .viewCount(post.getViewCount())
                .createdAt(post.getCreatedAt())
                .build();
    }

    private PostDetailResponse mapToDetail(ForumPost post, UUID viewerId) {
        boolean liked = viewerId != null && likeRepository
                .existsByUser_IdAndTargetTypeAndTargetId(viewerId, TargetType.POST, post.getId());
        boolean bookmarked = viewerId != null && bookmarkRepository
                .existsByUser_IdAndPost_Id(viewerId, post.getId());

        List<AttachedDocumentResponse> docs = postDocumentRepository
                .findByPost_IdAndCommentIsNull(post.getId())
                .stream()
                .map(link -> {
                    Document d = link.getDocument();
                    return AttachedDocumentResponse.builder()
                            .documentId(d.getId())
                            .documentName(d.getDocumentName())
                            .fileType(d.getFileType())
                            .previewUrl(d.getPreviewUrl())
                            .downloadUrl(d.getDownloadUrl())
                            .build();
                })
                .toList();

        return PostDetailResponse.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .authorId(post.getAuthor().getId())
                .authorName(resolveAuthorName(post.getAuthor()))
                .categoryId(post.getCategory() != null ? post.getCategory().getId() : null)
                .categoryName(post.getCategory() != null ? post.getCategory().getName() : null)
                .tags(post.getTags().stream().map(ForumTag::getName).toList())
                .status(post.getStatus().name())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .viewCount(post.getViewCount())
                .likedByMe(liked)
                .bookmarkedByMe(bookmarked)
                .attachedDocuments(docs)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    private String resolveAuthorName(User user) {
        if (user.getCustomerProfile() != null && user.getCustomerProfile().getFullName() != null) {
            return user.getCustomerProfile().getFullName();
        }
        return user.getEmail();
    }
}

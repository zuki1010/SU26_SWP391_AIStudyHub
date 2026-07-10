package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.aistudyhub.dto.request.ForumPostRequestDTO;
import swp391.aistudyhub.dto.response.ForumPostResponseDTO;
import swp391.aistudyhub.entity.ForumPost;
import swp391.aistudyhub.entity.ForumPostRevision;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.repository.CustomerProfileRepository;
import swp391.aistudyhub.repository.ForumPostRepository;
import swp391.aistudyhub.repository.ForumPostRevisionRepository;
import swp391.aistudyhub.repository.UserRepository;
import swp391.aistudyhub.service.ForumPostService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ForumPostServiceImpl implements ForumPostService {

    @Autowired
    private ForumPostRepository forumPostRepository;

    @Autowired
    private ForumPostRevisionRepository forumPostRevisionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    private User getCurrentUser() {
        Authentication au = SecurityContextHolder.getContext().getAuthentication();
        if (au == null || !au.isAuthenticated() || "anonymousUser".equals(au.getPrincipal().toString())) {
            throw new RuntimeException("You are not login yet!");
        }
        return userRepository.findByEmailIgnoreCase(au.getName())
                .orElseThrow(() -> new RuntimeException("This user is not found!"));
    }

    private String resolveUserName(User user) {
        return customerProfileRepository.findByUser_Id(user.getId())
                .map(profile -> profile.getFullName())
                .orElse(user.getEmail());
    }

    @Override
    @Transactional
    public ForumPostResponseDTO createPost(ForumPostRequestDTO requestDTO) {
        if (requestDTO.getTitle() == null || requestDTO.getTitle().trim().isEmpty()) {
            throw new RuntimeException("Tiêu đề bài viết không được để trống!");
        }
        if (requestDTO.getContent() == null || requestDTO.getContent().trim().isEmpty()) {
            throw new RuntimeException("Nội dung bài viết không được để trống!");
        }

        User user = getCurrentUser();

        ForumPost post = new ForumPost();
        post.setUserId(user.getId());
        post.setDocumentId(requestDTO.getDocumentId());
        post.setUserName(resolveUserName(user));
        post.setTitle(requestDTO.getTitle().trim());
        post.setContent(requestDTO.getContent().trim());
        post.setVisibility(requestDTO.getVisibility() != null ? requestDTO.getVisibility() : "PUBLIC");
        post.setStatus("ACTIVE");
        post.setIsPinned(false);
        post.setCreatedAt(Instant.now());
        post.setUpdatedAt(Instant.now());

        return toResponseDTO(forumPostRepository.save(post));
    }

    @Override
    public List<ForumPostResponseDTO> getAllPosts() {
        return forumPostRepository.findAllByOrderByIsPinnedDescCreatedAtDesc()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ForumPostResponseDTO getPostById(UUID postId) {
        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết!"));
        return toResponseDTO(post);
    }

    @Override
    @Transactional
    public ForumPostResponseDTO updatePost(UUID postId, ForumPostRequestDTO requestDTO) {
        User user = getCurrentUser();

        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết!"));

        if (post.getUserId() != null && !post.getUserId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa bài viết này!");
        }

        // Lưu lại phiên bản cũ vào forum_post_revisions trước khi cập nhật
        ForumPostRevision revision = new ForumPostRevision();
        revision.setPostId(post.getId());
        revision.setRevisionNo(UUID.randomUUID());
        revision.setTitle(post.getTitle());
        revision.setContent(post.getContent());
        revision.setStatus(post.getStatus());
        revision.setEditedBy(user.getId());
        revision.setEditedByName(resolveUserName(user));
        revision.setCreatedAt(Instant.now());
        forumPostRevisionRepository.save(revision);

        if (requestDTO.getTitle() != null && !requestDTO.getTitle().trim().isEmpty()) {
            post.setTitle(requestDTO.getTitle().trim());
        }
        if (requestDTO.getContent() != null && !requestDTO.getContent().trim().isEmpty()) {
            post.setContent(requestDTO.getContent().trim());
        }
        if (requestDTO.getVisibility() != null) {
            post.setVisibility(requestDTO.getVisibility());
        }
        post.setUpdatedAt(Instant.now());

        return toResponseDTO(forumPostRepository.save(post));
    }

    @Override
    @Transactional
    public ForumPostResponseDTO toggleVisibility(UUID postId) {
        User user = getCurrentUser();

        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết!"));

        if (post.getUserId() != null && !post.getUserId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền thay đổi hiển thị bài viết này!");
        }

        String current = post.getVisibility();
        if (current == null || "PUBLIC".equalsIgnoreCase(current)) {
            post.setVisibility("PRIVATE");
        } else {
            post.setVisibility("PUBLIC");
        }
        post.setUpdatedAt(Instant.now());

        return toResponseDTO(forumPostRepository.save(post));
    }

    private ForumPostResponseDTO toResponseDTO(ForumPost post) {
        ForumPostResponseDTO dto = new ForumPostResponseDTO();
        dto.setId(post.getId());
        dto.setDocumentId(post.getDocumentId());
        dto.setUserId(post.getUserId());
        dto.setUserName(post.getUserName());
        dto.setVisibility(post.getVisibility());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setStatus(post.getStatus());
        dto.setIsPinned(post.getIsPinned());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());
        return dto;
    }
}

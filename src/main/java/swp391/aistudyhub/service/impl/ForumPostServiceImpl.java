package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.aistudyhub.dto.request.ForumPostRequestDTO;
import swp391.aistudyhub.dto.response.ForumPostResponseDTO;
import swp391.aistudyhub.entity.ForumPost;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.repository.CustomerProfileRepository;
import swp391.aistudyhub.repository.ForumPostRepository;
import swp391.aistudyhub.repository.UserRepository;
import swp391.aistudyhub.service.ForumPostService;

import java.time.Instant;

@Service
public class ForumPostServiceImpl implements ForumPostService {

    @Autowired
    private ForumPostRepository forumPostRepository;

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

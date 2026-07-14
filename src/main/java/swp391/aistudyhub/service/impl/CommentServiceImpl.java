package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.aistudyhub.dto.request.CommentRequestDTO;
import swp391.aistudyhub.dto.response.CommentResponseDTO;
import swp391.aistudyhub.entity.Comment;
import swp391.aistudyhub.entity.Document;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.repository.CommentRepository;
import swp391.aistudyhub.repository.DocumentRepository;
import swp391.aistudyhub.repository.UserRepository;
import swp391.aistudyhub.service.CommentService;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    // Điều kiện 1: user phải đăng nhập (lấy từ token, giống ForumPostServiceImpl)
    private User getCurrentUser() {
        Authentication au = SecurityContextHolder.getContext().getAuthentication();
        if (au == null || !au.isAuthenticated() || "anonymousUser".equals(au.getPrincipal().toString())) {
            throw new RuntimeException("You are not login yet!");
        }
        return userRepository.findByEmailIgnoreCase(au.getName())
                .orElseThrow(() -> new RuntimeException("This user is not found!"));
    }

    // Điều kiện 2: tài liệu phải được admin duyệt public thì comment mới hiển thị
    private boolean isDocumentPublic(UUID documentId) {
        return documentRepository.findById(documentId)
                .map(Document::isPublic)
                .orElse(false);
    }

    @Override
    @Transactional
    public CommentResponseDTO createComment(CommentRequestDTO requestDTO) {
        if (requestDTO.getContent() == null || requestDTO.getContent().trim().isEmpty()) {
            throw new RuntimeException("Nội dung bình luận không được để trống!");
        }
        if (requestDTO.getDocumentId() == null || !documentRepository.existsById(requestDTO.getDocumentId())) {
            throw new RuntimeException("Không tìm thấy tài liệu!");
        }

        User user = getCurrentUser();

        Comment comment = new Comment();
        comment.setDocumentId(requestDTO.getDocumentId());
        comment.setUserId(user.getId());
        comment.setContent(requestDTO.getContent().trim());
        comment.setCreatedAt(Instant.now());

        return toResponseDTO(commentRepository.save(comment));
    }

    @Override
    public List<CommentResponseDTO> getCommentsByDocument(UUID documentId) {
        // Tài liệu chưa được duyệt public -> không hiện gì hết
        if (!isDocumentPublic(documentId)) {
            return Collections.emptyList();
        }
        return commentRepository.findByDocumentIdOrderByCreatedAtDesc(documentId)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    private CommentResponseDTO toResponseDTO(Comment comment) {
        CommentResponseDTO dto = new CommentResponseDTO();
        dto.setCommentId(comment.getCommentId());
        dto.setDocumentId(comment.getDocumentId());
        dto.setUserId(comment.getUserId());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt());
        return dto;
    }
}

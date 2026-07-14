package swp391.aistudyhub.service;

import swp391.aistudyhub.dto.request.CommentRequestDTO;
import swp391.aistudyhub.dto.response.CommentResponseDTO;

import java.util.List;
import java.util.UUID;

public interface CommentService {

    CommentResponseDTO createComment(CommentRequestDTO requestDTO);

    List<CommentResponseDTO> getCommentsByDocument(UUID documentId);

    CommentResponseDTO updateComment(UUID commentId, String content);

    void deleteComment(UUID commentId);
}

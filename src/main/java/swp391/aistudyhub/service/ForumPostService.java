package swp391.aistudyhub.service;

import swp391.aistudyhub.dto.request.ForumPostRequestDTO;
import swp391.aistudyhub.dto.response.ForumPostResponseDTO;

import java.util.List;
import java.util.UUID;

public interface ForumPostService {

    ForumPostResponseDTO createPost(ForumPostRequestDTO requestDTO);

    List<ForumPostResponseDTO> getAllPosts();

    ForumPostResponseDTO getPostById(UUID postId);

    ForumPostResponseDTO updatePost(UUID postId, ForumPostRequestDTO requestDTO);

    void deletePost(UUID postId);

    ForumPostResponseDTO toggleVisibility(UUID postId);
}

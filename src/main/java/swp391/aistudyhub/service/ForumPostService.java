package swp391.aistudyhub.service;

import swp391.aistudyhub.dto.request.ForumPostRequestDTO;
import swp391.aistudyhub.dto.response.ForumPostResponseDTO;

public interface ForumPostService {

    ForumPostResponseDTO createPost(ForumPostRequestDTO requestDTO);
}

package swp391.aistudyhub.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import swp391.aistudyhub.dto.request.CreatePostRequest;
import swp391.aistudyhub.dto.request.UpdatePostRequest;
import swp391.aistudyhub.dto.response.PostDetailResponse;
import swp391.aistudyhub.dto.response.PostSummaryResponse;

import java.util.UUID;

public interface ForumPostService {

    PostDetailResponse createPost(UUID userId, CreatePostRequest request);

    PostDetailResponse updatePost(UUID userId, UUID postId, UpdatePostRequest request);

    void deletePost(UUID userId, UUID postId);

    /**
     * Listing with optional category filter and sort mode ("newest" or "hot").
     */
    Page<PostSummaryResponse> listPosts(UUID categoryId, String sort, Pageable pageable);

    Page<PostSummaryResponse> searchPosts(String keyword, Pageable pageable);

    /**
     * Detail view; increments the view counter and resolves like/bookmark state for the viewer.
     * viewerId may be null for guests.
     */
    PostDetailResponse getPostDetail(UUID postId, UUID viewerId);
}

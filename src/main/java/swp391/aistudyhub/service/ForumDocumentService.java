package swp391.aistudyhub.service;

import org.springframework.core.io.Resource;
import swp391.aistudyhub.dto.request.AttachDocumentRequest;
import swp391.aistudyhub.dto.response.AttachedDocumentResponse;

import java.util.List;
import java.util.UUID;

public interface ForumDocumentService {

    /**
     * Attaches documents (owned by the user) to a post or one of its comments,
     * making each attached document public.
     */
    List<AttachedDocumentResponse> attachDocuments(UUID userId, UUID postId, AttachDocumentRequest request);

    List<AttachedDocumentResponse> getPostDocuments(UUID postId);

    List<AttachedDocumentResponse> getCommentDocuments(UUID commentId);

    /**
     * Streams a forum-shared document file. Accessible to anyone since the document is public.
     */
    Resource downloadSharedDocument(UUID documentId);

    AttachedDocumentResponse getSharedDocumentMeta(UUID documentId);
}

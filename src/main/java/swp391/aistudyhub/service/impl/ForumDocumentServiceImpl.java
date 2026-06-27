package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.aistudyhub.dto.request.AttachDocumentRequest;
import swp391.aistudyhub.dto.response.AttachedDocumentResponse;
import swp391.aistudyhub.entity.Document;
import swp391.aistudyhub.exception.AuthException;
import swp391.aistudyhub.repository.DocumentRepository;
import swp391.aistudyhub.service.ForumDocumentService;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ForumDocumentServiceImpl implements ForumDocumentService {

    @Autowired
    private ForumPostDocumentRepository postDocumentRepository;

    @Autowired
    private ForumPostRepository postRepository;

    @Autowired
    private ForumCommentRepository commentRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Override
    @Transactional
    public List<AttachedDocumentResponse> attachDocuments(UUID userId, UUID postId, AttachDocumentRequest request) {
        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new AuthException("Không tìm thấy bài đăng.", HttpStatus.NOT_FOUND));

        ForumComment comment = null;
        if (request.getCommentId() != null) {
            comment = commentRepository.findById(request.getCommentId())
                    .orElseThrow(() -> new AuthException("Không tìm thấy bình luận.", HttpStatus.NOT_FOUND));
            if (!Objects.equals(comment.getPost().getId(), postId)) {
                throw new AuthException("Bình luận không thuộc bài đăng này.", HttpStatus.BAD_REQUEST);
            }
            // Only the comment author can attach documents to it.
            if (!Objects.equals(comment.getAuthor().getId(), userId)) {
                throw new AuthException("Bạn không có quyền đính kèm vào bình luận này.", HttpStatus.FORBIDDEN);
            }
        } else {
            // Attaching to the post body: only the post author may do so.
            if (!Objects.equals(post.getAuthor().getId(), userId)) {
                throw new AuthException("Bạn không có quyền đính kèm vào bài đăng này.", HttpStatus.FORBIDDEN);
            }
        }

        for (UUID docId : request.getDocumentIds()) {
            Document document = documentRepository.findByIdAndUserId(docId, userId)
                    .orElseThrow(() -> new AuthException(
                            "Tài liệu không tồn tại hoặc không thuộc về bạn: " + docId, HttpStatus.BAD_REQUEST));

            // Sharing into the forum makes the document public.
            if (!document.isPublic()) {
                document.setPublic(true);
                documentRepository.save(document);
            }

            ForumPostDocument link = new ForumPostDocument();
            link.setPost(post);
            link.setComment(comment);
            link.setDocument(document);
            postDocumentRepository.save(link);
        }

        return request.getCommentId() != null
                ? getCommentDocuments(request.getCommentId())
                : getPostDocuments(postId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachedDocumentResponse> getPostDocuments(UUID postId) {
        return postDocumentRepository.findByPost_IdAndCommentIsNull(postId).stream()
                .map(link -> toResponse(link.getDocument()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachedDocumentResponse> getCommentDocuments(UUID commentId) {
        return postDocumentRepository.findByComment_Id(commentId).stream()
                .map(link -> toResponse(link.getDocument()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadSharedDocument(UUID documentId) {
        Document doc = requirePublicDocument(documentId);
        try {
            java.net.URL url = java.net.URI.create(doc.getDownloadUrl()).toURL();
            try (InputStream inputStream = url.openStream()) {
                return new ByteArrayResource(inputStream.readAllBytes());
            }
        } catch (Exception e) {
            throw new AuthException("Không thể tải tài liệu: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AttachedDocumentResponse getSharedDocumentMeta(UUID documentId) {
        return toResponse(requirePublicDocument(documentId));
    }

    // ---------- helpers ----------

    private Document requirePublicDocument(UUID documentId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new AuthException("Không tìm thấy tài liệu.", HttpStatus.NOT_FOUND));
        if (!doc.isPublic()) {
            throw new AuthException("Tài liệu này không được chia sẻ công khai.", HttpStatus.FORBIDDEN);
        }
        return doc;
    }

    private AttachedDocumentResponse toResponse(Document d) {
        return AttachedDocumentResponse.builder()
                .documentId(d.getId())
                .documentName(d.getDocumentName())
                .fileType(d.getFileType())
                .previewUrl(d.getPreviewUrl())
                .downloadUrl(d.getDownloadUrl())
                .build();
    }
}

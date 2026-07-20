package swp391.aistudyhub.service;

import org.springframework.core.io.Resource;
import swp391.aistudyhub.dto.request.DocumentRequestDTO;
import swp391.aistudyhub.dto.response.DocumentResponseDTO;
import swp391.aistudyhub.enums.RequestPublicDoc;

import java.util.List;
import java.util.UUID;

public interface DocumentService {

    DocumentResponseDTO createDocument(DocumentRequestDTO requestDTO);

    List<DocumentResponseDTO> getAllDocumentsByUser();

    DocumentResponseDTO getDocumentDetail(UUID documentId);

    DocumentResponseDTO updateDocumentName(UUID documentId, String newName);

    void deleteDocument(UUID documentId);

    Resource downloadDocumentFile(UUID documentId);

    Resource getFileResourceForPreview(UUID documentId);

    List<DocumentResponseDTO> searchDocumentsByFilter(String name);

    DocumentResponseDTO toggleDocumentPublicStatus(UUID documentId, boolean isPublic);

    List<DocumentResponseDTO> getPublicDocuments();

    DocumentResponseDTO approvePublicRequest(UUID documentId, RequestPublicDoc decision);

    long getTotalQuota();
}
package swp391.aistudyhub.service;

import org.springframework.core.io.Resource;
import swp391.aistudyhub.dto.request.DocumentRequestDTO;
import swp391.aistudyhub.dto.response.DocumentResponseDTO;
import swp391.aistudyhub.entity.Document;

import java.util.List;
import java.util.UUID;

public interface DocumentService {

    DocumentResponseDTO createDocument(UUID userId, DocumentRequestDTO requestDTO);

    List<DocumentResponseDTO> getAllDocumentsByUserId(UUID userId);

    DocumentResponseDTO getDocumentDetail(UUID documentId, UUID userId);

    DocumentResponseDTO updateDocumentName(UUID documentId, UUID userId, String newName);

    void deleteDocument(UUID documentId, UUID userId);

    Resource downloadDocumentFile(UUID documentId, UUID userId);

    List<Document> getMyDocuments();

    List<DocumentResponseDTO> searchAndFilterDocuments(
            UUID userId,
            String searchName,
            String fileType
    );

    List<DocumentResponseDTO> getPublicDocuments();

    DocumentResponseDTO updateDocumentVisibility(
            UUID documentId,
            UUID userId,
            Boolean isPublic
    );
}
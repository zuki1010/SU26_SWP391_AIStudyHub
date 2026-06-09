package swp391.aistudyhub.service;

import swp391.aistudyhub.dto.DocumentRequestDTO;
import swp391.aistudyhub.dto.DocumentResponseDTO;
import java.util.List;
import java.util.UUID;

public interface DocumentService {
    DocumentResponseDTO createDocument(UUID userId, DocumentRequestDTO requestDTO);
    List<DocumentResponseDTO> getAllDocumentsByUserId(UUID userId);
    DocumentResponseDTO getDocumentDetail(UUID documentId, UUID userId);
    DocumentResponseDTO updateDocumentName(UUID documentId, UUID userId, String newName);
    void deleteDocument(UUID documentId, UUID userId, Long fileSize);
}

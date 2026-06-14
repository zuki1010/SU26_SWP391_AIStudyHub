package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.aistudyhub.dto.DocumentRequestDTO;
import swp391.aistudyhub.dto.DocumentResponseDTO;
import swp391.aistudyhub.entity.Document;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.repository.DocumentRepository;
import swp391.aistudyhub.service.DocumentService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentServiceImpl implements DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Override
    @Transactional
    public DocumentResponseDTO createDocument(UUID userId, DocumentRequestDTO requestDTO) {
        User user = new User();
        user.setId(userId);

        Document doc = new Document();
        doc.setUser(user);
        doc.setDocumentName(requestDTO.getDocumentName());
        doc.setFileType(requestDTO.getFileType());
        doc.setPreviewUrl(requestDTO.getPreviewUrl());
        doc.setDownloadUrl(requestDTO.getDownloadUrl());
        doc.setCreatedAt(Instant.now());

        Document savedDoc = documentRepository.save(doc);
        return mapToResponseDTO(savedDoc);
    }

    @Override
    public List<DocumentResponseDTO> getAllDocumentsByUserId(UUID userId) {
        // Tạm thời trả về danh sách rỗng để dự án không bị lỗi compile
        // Bạn có thể bổ sung logic gọi repository.findByUserId(userId) của nhóm bạn tại đây sau
        return new ArrayList<>();
    }

    @Override
    public DocumentResponseDTO getDocumentDetail(UUID documentId, UUID userId) {
        // Tạm thời trả về null hoặc bạn thay bằng logic lấy chi tiết tài liệu cũ của nhóm
        return null;
    }

    @Override
    @Transactional
    public DocumentResponseDTO updateDocumentName(UUID documentId, UUID userId, String newName) {
        // Tạm thời trả về null để sửa lỗi đỏ biên dịch trước
        return null;
    }

    @Override
    @Transactional
    public void deleteDocument(UUID documentId, UUID userId, Long fileSize) {
        // Tạm thời để trống logic xóa
    }

    @Override
    public Resource downloadDocumentFile(UUID documentId, UUID userId) {
        // Tạm thời trả về null cho hàm tải file
        return null;
    }

    private DocumentResponseDTO mapToResponseDTO(Document document) {
        DocumentResponseDTO dto = new DocumentResponseDTO();
        dto.setDocumentId(document.getId());
        dto.setDocumentName(document.getDocumentName());
        dto.setFileType(document.getFileType());
        dto.setPreviewUrl(document.getPreviewUrl());
        dto.setDownloadUrl(document.getDownloadUrl());
        return dto;
    }
}
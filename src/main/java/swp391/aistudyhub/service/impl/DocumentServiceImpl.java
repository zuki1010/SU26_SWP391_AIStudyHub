package swp391.aistudyhub.service.impl;

import org.springframework.core.io.Resource;
import swp391.aistudyhub.dto.DocumentRequestDTO;
import swp391.aistudyhub.dto.DocumentResponseDTO;
import swp391.aistudyhub.entity.Document;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.repository.DocumentRepository;
import swp391.aistudyhub.service.DocumentService;
import org.springframework.core.io.ByteArrayResource;
import java.io.InputStream;
import java.net.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DocumentServiceImpl implements DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    private DocumentResponseDTO mapToResponseDTO(Document doc) {
        DocumentResponseDTO dto = new DocumentResponseDTO();
        dto.setDocumentId(doc.getId());
        dto.setDocumentName(doc.getDocumentName());
        dto.setFileType(doc.getFileType());
        dto.setPreviewUrl(doc.getPreviewUrl());
        dto.setDownloadUrl(doc.getDownloadUrl());
        dto.setCreatedAt(doc.getCreatedAt());
        return dto;
    }

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

    @Transactional(readOnly = true)
    @Override
    public List<DocumentResponseDTO> getAllDocumentsByUserId(UUID userId) {
        List<Document> documents = documentRepository.findByUserId(userId);
        return documents.stream().map(this::mapToResponseDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponseDTO getDocumentDetail(UUID documentId, UUID userId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy tài liệu này!"));

        if (!doc.getUser().getId().equals(userId)) {
            throw new RuntimeException("Từ chối: Bạn không có quyền truy cập tài liệu này!");
        }
        return mapToResponseDTO(doc);
    }

    @Override
    @Transactional
    public DocumentResponseDTO updateDocumentName(UUID documentId, UUID userId, String newName) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy tài liệu cần sửa!"));

        if (!doc.getUser().getId().equals(userId)) {
            throw new RuntimeException("Từ chối: Bạn không có quyền chỉnh sửa tài liệu này!");
        }

        doc.setDocumentName(newName);
        Document updatedDoc = documentRepository.save(doc);
        return mapToResponseDTO(updatedDoc);
    }

    @Override
    @Transactional
    public void deleteDocument(UUID documentId, UUID userId, Long fileSize) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy tài liệu để xóa!"));

        if (!doc.getUser().getId().equals(userId)) {
            throw new RuntimeException("Từ chối: Bạn không có quyền xóa tài liệu này!");
        }

        documentRepository.delete(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadDocumentFile(UUID documentId, UUID userId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy tài liệu để tải!"));

        if (!doc.getUser().getId().equals(userId)) {
            throw new RuntimeException("Từ chối: Bạn không có quyền tải tài liệu này!");
        }

        try {
            URL url = new URL(doc.getDownloadUrl());

            try (InputStream in = url.openStream()) {
                byte[] fileBytes = in.readAllBytes();


                return new ByteArrayResource(fileBytes);
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tải file từ bộ lưu trữ Cloud: " + e.getMessage());
        }
    }
}
package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.aistudyhub.dto.DocumentRequestDTO;
import swp391.aistudyhub.dto.DocumentResponseDTO;
import swp391.aistudyhub.entity.CloudStorage;
import swp391.aistudyhub.entity.Document;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.repository.DocumentChunkRepository;
import swp391.aistudyhub.repository.DocumentRepository;
import swp391.aistudyhub.repository.CloudStorageRepository; // 1. ĐÃ THÊM IMPORT
import swp391.aistudyhub.service.DocumentService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class DocumentServiceImpl implements DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @Autowired
    private CloudStorageRepository cloudStorageRepository; // 2. ĐÃ TIÊM REPOSITORY ĐỂ HẾT LỖI ĐỎ

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
        // 3. ĐÃ SỬA: Đổi findByUserId thành findByUser_Id để đúng quy tắc ánh xạ JPA
        return documentRepository.findByUser_Id(userId)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    @Override
    public DocumentResponseDTO getDocumentDetail(UUID documentId, UUID userId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        if (doc.getUser() == null || !Objects.equals(doc.getUser().getId(), userId)) {
            throw new RuntimeException("Bạn không có quyền xem tài liệu này");
        }

        return mapToResponseDTO(doc);
    }

    @Override
    @Transactional
    public DocumentResponseDTO updateDocumentName(UUID documentId, UUID userId, String newName) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        if (doc.getUser() == null || !Objects.equals(doc.getUser().getId(), userId)) {
            throw new RuntimeException("Bạn không có quyền sửa tài liệu này");
        }

        doc.setDocumentName(newName);
        Document updatedDoc = documentRepository.save(doc);

        return mapToResponseDTO(updatedDoc);
    }

    @Override
    @Transactional
    public void deleteDocument(UUID documentId, UUID userId, Long fileSize) {
        // 4. ĐÃ SỬA: Thay thế findByIdAndUserId thành findByIdAndUser_Id để JPA hiểu liên kết bảng
        // Đồng thời đổi ResourceNotFoundException thành RuntimeException
        Document document = documentRepository.findByIdAndUser_Id(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Tài liệu không tồn tại hoặc bạn không có quyền xóa"));

        // 5. Xóa các chunk dữ liệu liên quan
        documentChunkRepository.deleteByDocument_Id(documentId);

        // 6. Xóa bản ghi tài liệu chính
        documentRepository.delete(document);

        // 7. Đồng bộ Cloud Storage (Dùng RuntimeException thay thế)
        CloudStorage storage = cloudStorageRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Cấu hình lưu trữ đám mây không tồn tại"));

        long newUsedQuota = Math.max(0, storage.getUsedQuota() - fileSize);
        storage.setUsedQuota(newUsedQuota);
        cloudStorageRepository.save(storage);
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

    @Override
    public Resource downloadDocumentFile(UUID documentId, UUID userId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        if (doc.getUser() == null || !Objects.equals(doc.getUser().getId(), userId)) {
            throw new RuntimeException("Bạn không có quyền truy cập tài liệu này");
        }

        try {
            Path filePath = Paths.get(doc.getDownloadUrl());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File không tồn tại trên hệ thống lưu trữ");
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi đọc file: " + e.getMessage());
        }
    }
}
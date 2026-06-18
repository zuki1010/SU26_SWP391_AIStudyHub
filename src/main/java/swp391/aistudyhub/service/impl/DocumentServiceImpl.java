package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.aistudyhub.dto.DocumentRequestDTO;
import swp391.aistudyhub.dto.DocumentResponseDTO;
import swp391.aistudyhub.entity.CloudStorage;
import swp391.aistudyhub.entity.Document;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.repository.CloudStorageRepository;
import swp391.aistudyhub.repository.DocumentChunkRepository;
import swp391.aistudyhub.repository.DocumentRepository;
import swp391.aistudyhub.repository.UserRepository;
import swp391.aistudyhub.service.DocumentService;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class DocumentServiceImpl implements DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private CloudStorageRepository cloudStorageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @Override
    @Transactional
    public DocumentResponseDTO createDocument(UUID userId, DocumentRequestDTO requestDTO) {
        // 1. Tìm User thực tế (Vì Document thuộc về User theo ERD)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng hệ thống."));

        // 2. Tìm CloudStorage của User đó (Gọi thông qua trục chung là userId)
        CloudStorage storage = cloudStorageRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu hình không gian lưu trữ của người dùng này."));

        // 3. Tạo và lưu Document trực tiếp cho User
        Document doc = new Document();
        doc.setUser(user); // Khớp 100% với cột user_id dưới Supabase
        doc.setDocumentName(requestDTO.getDocumentName());
        doc.setFileType(requestDTO.getFileType());
        doc.setPreviewUrl(requestDTO.getPreviewUrl());
        doc.setDownloadUrl(requestDTO.getDownloadUrl());
        Document savedDoc = documentRepository.save(doc);

        // 4. LOGIC QUOTA: Giả định dung lượng file mới truyền lên (ví dụ: mặc định tạm thời là 10MB = 10485760 bytes để test)
        long newFileSize = 10485760L;

        // Cộng dồn dung lượng đã dùng vào CloudStorage của User đó
        long updatedUsedQuota = storage.getUsedQuota() + newFileSize;

        // Kiểm tra xem có bị tràn bộ nhớ (Over quota) không
        if (updatedUsedQuota > storage.getTotalQuota()) {
            throw new RuntimeException("Không gian lưu trữ đám mây của bạn đã đầy!");
        }

        storage.setUsedQuota(updatedUsedQuota);
        cloudStorageRepository.save(storage); // Cập nhật lại dung lượng mới xuống Supabase

        return mapToResponseDTO(savedDoc);
    }

    @Transactional(readOnly = true)
    @Override
    public List<DocumentResponseDTO> getAllDocumentsByUserId(UUID userId) {
        // ĐÃ ĐỔI: Gọi findByUserId (bỏ gạch dưới) khớp với Repository
        return documentRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
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
        // ĐÃ ĐỔI: Gọi findByIdAndUserId (bỏ gạch dưới) khớp với Repository
        Document document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Tài liệu không tồn tại hoặc bạn không có quyền xóa"));

        documentChunkRepository.deleteByDocument_Id(documentId);
        documentRepository.delete(document);

        // Khấu trừ hoàn trả dung lượng bộ nhớ cho User
        CloudStorage storage = cloudStorageRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Cấu hình lưu trữ đám mây không tồn tại"));

        long validatedFileSize = (fileSize != null) ? Math.max(0, fileSize) : 0L;
        long newUsedQuota = Math.max(0, storage.getUsedQuota() - validatedFileSize);

        storage.setUsedQuota(newUsedQuota);
        cloudStorageRepository.save(storage);
    }

    public List<Document> getMyDocuments() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Người dùng chưa được xác thực hệ thống");
        }

        String currentUserEmail = authentication.getName();

        User user = userRepository.findByEmailIgnoreCase(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản: " + currentUserEmail));

        // ĐÃ ĐỔI: Gọi findByUserId (bỏ gạch dưới) khớp với Repository
        return documentRepository.findByUserId(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadDocumentFile(UUID documentId, UUID userId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        if (doc.getUser() == null || !Objects.equals(doc.getUser().getId(), userId)) {
            throw new RuntimeException("Bạn không có quyền truy cập tài liệu này");
        }

        try {
            String stringUrl = doc.getDownloadUrl();
            java.net.URL url = java.net.URI.create(stringUrl).toURL();

            try (InputStream inputStream = url.openStream()) {
                byte[] bytes = inputStream.readAllBytes();
                return new ByteArrayResource(bytes);
            }
        } catch (Exception e) {
            throw new RuntimeException("Không thể tải file từ Cloud Storage: " + e.getMessage());
        }
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
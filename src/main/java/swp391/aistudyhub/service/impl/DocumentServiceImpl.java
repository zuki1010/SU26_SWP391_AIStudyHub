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
import java.net.URL;
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
        // Lấy cấu hình bộ nhớ CloudStorage của User ra trước để gán quan hệ
        CloudStorage storage = cloudStorageRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu hình không gian lưu trữ của người dùng này."));

        Document doc = new Document();
        doc.setStorage(storage); // ĐÃ ĐỔI: Gán đúng thực thể Storage theo đúng Entity của bạn
        doc.setDocumentName(requestDTO.getDocumentName());
        doc.setFileType(requestDTO.getFileType());
        doc.setPreviewUrl(requestDTO.getPreviewUrl());
        doc.setDownloadUrl(requestDTO.getDownloadUrl());

        Document savedDoc = documentRepository.save(doc);
        return mapToResponseDTO(savedDoc);
    }

    @Transactional(readOnly = true)
    @Override
    public List<DocumentResponseDTO> getAllDocumentsByUserId(UUID userId) {
        // Gọi hàm truy vấn xuyên bảng đã sửa ở Bước 1
        return documentRepository.findByStorage_User_Id(userId)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponseDTO getDocumentDetail(UUID documentId, UUID userId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        // Kiểm tra bảo mật quyền sở hữu thông qua thực thể trung gian Storage -> User
        if (doc.getStorage() == null || doc.getStorage().getUser() == null ||
                !Objects.equals(doc.getStorage().getUser().getId(), userId)) {
            throw new RuntimeException("Bạn không có quyền xem tài liệu này");
        }

        return mapToResponseDTO(doc);
    }

    @Override
    @Transactional
    public DocumentResponseDTO updateDocumentName(UUID documentId, UUID userId, String newName) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        if (doc.getStorage() == null || doc.getStorage().getUser() == null ||
                !Objects.equals(doc.getStorage().getUser().getId(), userId)) {
            throw new RuntimeException("Bạn không có quyền sửa tài liệu này");
        }

        doc.setDocumentName(newName);
        Document updatedDoc = documentRepository.save(doc);

        return mapToResponseDTO(updatedDoc);
    }

    @Override
    @Transactional
    public void deleteDocument(UUID documentId, UUID userId, Long fileSize) {
        // Tìm và xóa dựa trên hàm liên kết cấu trúc thực tế
        Document document = documentRepository.findByIdAndStorage_User_Id(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Tài liệu không tồn tại hoặc bạn không có quyền xóa"));

        documentChunkRepository.deleteByDocument_Id(documentId);
        documentRepository.delete(document);

        // Khấu trừ hoàn trả dung lượng bộ nhớ đám mây đám mây địa phương cho User
        CloudStorage storage = cloudStorageRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Cấu hình lưu trữ đám mây không tồn tại"));
        // Bảo vệ dữ liệu: Đảm bảo đầu vào fileSize không bị âm
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

        CloudStorage storage = cloudStorageRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy không gian lưu trữ cho tài khoản này"));

        return documentRepository.findAllByStorage(storage);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadDocumentFile(UUID documentId, UUID userId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        if (doc.getStorage() == null || doc.getStorage().getUser() == null ||
                !Objects.equals(doc.getStorage().getUser().getId(), userId)) {
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
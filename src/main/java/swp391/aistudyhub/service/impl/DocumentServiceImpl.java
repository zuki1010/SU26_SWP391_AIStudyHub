package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng hệ thống."));

        CloudStorage storage = cloudStorageRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu hình không gian lưu trữ của người dùng này."));

        long actualFileSize = requestDTO.getFileSize() != null ? requestDTO.getFileSize() : 0L;

        long updatedUsedQuota = storage.getUsedQuota() + actualFileSize;
        if (updatedUsedQuota > storage.getTotalQuota()) {
            throw new RuntimeException("Không gian lưu trữ đám mây của bạn đã đầy!");
        }

        Document doc = new Document();
        doc.setUser(user);
        doc.setDocumentName(requestDTO.getDocumentName());
        doc.setFileType(requestDTO.getFileType());
        doc.setPreviewUrl(requestDTO.getPreviewUrl());
        doc.setDownloadUrl(requestDTO.getDownloadUrl());
        doc.setFileSize(actualFileSize);

        Document savedDoc = documentRepository.saveAndFlush(doc);

//        cloudStorageRepository.plusUsedQuota(userId, actualFileSize);
//
//        entityManager.flush();
//        entityManager.clear();

// 4. Cập nhật quota mới của user
        storage.setUsedQuota(updatedUsedQuota);
        cloudStorageRepository.save(storage);
// ==========================================================

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

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Override
    @Transactional
    public void deleteDocument(UUID documentId, UUID userId) {
        Document document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Tài liệu không tồn tại hoặc bạn không có quyền xóa"));


        long actualFileSize = (document.getFileSize() != null) ? document.getFileSize() : 0L;


        CloudStorage storage = cloudStorageRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Cấu hình lưu trữ đám mây không tồn tại"));


        long newUsedQuota = Math.max(0, storage.getUsedQuota() - actualFileSize);
        storage.setUsedQuota(newUsedQuota);

        cloudStorageRepository.saveAndFlush(storage);

        entityManager.flush();
        entityManager.clear();


        documentChunkRepository.deleteByDocument_Id(documentId);
        documentRepository.delete(document);
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

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> searchAndFilterDocuments(UUID userId, String searchName, String fileType) {

        return documentRepository.findAll((root, query, cb) -> {
                    java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();

                    predicates.add(cb.equal(root.get("user").get("id"), userId));

                    if (searchName != null && !searchName.trim().isEmpty()) {
                        predicates.add(cb.like(cb.lower(root.get("documentName")), "%" + searchName.toLowerCase().trim() + "%"));
                    }


                    if (fileType != null && !fileType.trim().isEmpty()) {
                        predicates.add(cb.like(cb.lower(root.get("fileType")), "%" + fileType.toLowerCase().trim() + "%"));
                    }

                    query.orderBy(cb.desc(root.get("createdAt")));

                    return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
                })
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }
}
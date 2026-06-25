package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import swp391.aistudyhub.dto.request.DocumentRequestDTO;
import swp391.aistudyhub.dto.response.DocumentResponseDTO;
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
        doc.setDescription(requestDTO.getDescription());

        Document savedDoc = documentRepository.saveAndFlush(doc);

        long realTotalUsedQuota = documentRepository.sumFileSizeByUserId(userId);

        storage.setUsedQuota(updatedUsedQuota);
        cloudStorageRepository.save(storage);

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


    @Autowired
    private RestTemplate restTemplate;

    @Override
    @Transactional
    public void deleteDocument(UUID documentId, UUID userId) {
        Document document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Tài liệu không tồn tại hoặc bạn không có quyền xóa"));


        long actualFileSize = (document.getFileSize() != null) ? document.getFileSize() : 0L;

        try {
            // Giải pháp lấy tên file thông minh: Trích xuất tên file thực tế từ link downloadUrl đã lưu trong DB
            String downloadUrl = document.getDownloadUrl();
            if (downloadUrl == null || downloadUrl.trim().isEmpty()) {
                downloadUrl = document.getPreviewUrl();
            }

            // Tìm phần tên file vật lý nằm sau chữ /documents/
            String fileKey = "";
            if (downloadUrl != null && downloadUrl.contains("/documents/")) {
                fileKey = downloadUrl.substring(downloadUrl.indexOf("/documents/") + 11);
            } else {
                // Phương án dự phòng nếu không tìm thấy link dạng chuẩn: dùng userId/documentId.extension
                String extension = document.getFileType().toLowerCase().trim();
                fileKey = userId.toString() + "/" + documentId.toString() + "." + extension;
            }

            // Endpoint chính xác để gọi phương thức DELETE của Supabase Storage
            String supabaseUrl = "https://ybgeblpkptrsefpafthb.supabase.co/storage/v1/object/documents/" + fileKey;

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();

            // ⚠️ CHÚ Ý QUAN TRỌNG: Hãy dán mã Service Role Key của bạn vào đây (Bắt buộc dùng Service Role để có quyền xóa)
            String supabaseToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InliZ2VibHBrcHRyc2VmcGFmdGhiIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc4MDYwNDM0MSwiZXhwIjoyMDk2MTgwMzQxfQ.T0MpOxyT3aFSdVMy-o4j22D8OxO9hXzRLzDjkbputUI";

            headers.set("Authorization", "Bearer " + supabaseToken);
            headers.set("apiKey", supabaseToken);

            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            // Gửi lệnh xóa lên cloud Supabase
            restTemplate.exchange(supabaseUrl, org.springframework.http.HttpMethod.DELETE, entity, String.class);
            System.out.println("==> Đã xóa file vật lý thành công trên Supabase Bucket: " + fileKey);

        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String errorResponse = e.getResponseBodyAsString();
            // Nếu file thực tế trên Cloud vốn dĩ đã bị mất từ trước (404 Not Found), ta log cảnh báo và vẫn cho chạy tiếp xuống dưới để dọn DB
            if (e.getStatusCode().value() == 404 || errorResponse.contains("not_found")) {
                System.out.println("==> Cảnh báo: File không tìm thấy trên Cloud (404), tiến hành dọn dẹp tiếp Database.");
            } else {
                // Nếu gặp các lỗi khác (403 Unauthorized, 400 lỗi cú pháp hệ thống...), lập tức chặn đứng (ném RuntimeException) để Rollback Transaction, không cho xóa DB local!
                throw new RuntimeException("Không thể xóa file vật lý trên Supabase Bucket. Mã lỗi: " + e.getStatusCode() + " - Chi tiết: " + errorResponse);
            }
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("404") || msg.contains("not_found")) {
                System.out.println("==> Cảnh báo: Phát hiện mã lỗi 404, tiếp tục dọn dẹp Database.");
            } else {
                throw new RuntimeException("Lỗi kết nối mạng đến Supabase: " + e.getMessage());
            }
        }

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
    dto.setCreatedAt(document.getCreatedAt());

    // THÊM DÒNG NÀY ĐỂ FE PREVIEW ĐỌC ĐƯỢC
    dto.setDescription(document.getDescription());

    // Vì FE có thể đọc textContent, ta cho textContent = description luôn
    dto.setTextContent(document.getDescription());

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
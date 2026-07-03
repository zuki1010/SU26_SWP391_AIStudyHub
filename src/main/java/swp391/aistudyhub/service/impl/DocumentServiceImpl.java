package swp391.aistudyhub.service.impl;

import jakarta.persistence.PersistenceContext;
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
import swp391.aistudyhub.entity.*;
import swp391.aistudyhub.enums.SubjectCode;
import swp391.aistudyhub.repository.*;
import swp391.aistudyhub.service.DocumentService;
import swp391.aistudyhub.service.StorageUploadService;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private StorageUploadService storageUploadService;

    @Autowired
    private DocumentShareRepository documentShareRepository;


    @Override
    @Transactional
    public DocumentResponseDTO createDocument(DocumentRequestDTO requestDTO) {
        // 🌟 LẤY USER NGẦM TỪ TOKEN
        Authentication au = SecurityContextHolder.getContext().getAuthentication();
        if (au == null || !au.isAuthenticated() || "anonymousUser".equals(au.getPrincipal().toString())) {
            throw new RuntimeException("You are not login yet!");
        }
        User user = userRepository.findByEmailIgnoreCase(au.getName())
                .orElseThrow(() -> new RuntimeException("This user is not found!"));

        UUID userId = user.getId();

        CloudStorage storage = cloudStorageRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu hình không gian lưu trữ của người dùng này."));

        long actualFileSize = requestDTO.getFileSize() != null ? requestDTO.getFileSize() : 0L;

        long updatedUsedQuota = storage.getUsedQuota() + actualFileSize;
        if (updatedUsedQuota > storage.getTotalQuota()) {
            storageUploadService.logFailure(storage, requestDTO.getDocumentName(), actualFileSize, "FAILED_QUOTA_FULL");
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
        doc.setCategoryId(null); // 🌟 Giữ null ở bảng documents theo ý bạn để né xích khóa ngoại chéo

        Document savedDoc = documentRepository.saveAndFlush(doc);

        // 2. XỬ LÝ LƯU DANH MỤC (ĐẢM BẢO NHIỀU DOCUMENT DÙNG CHUNG 1 CATEGORY ĐÃ CÓ)
        if (requestDTO.getCategoryNames() != null && !requestDTO.getCategoryNames().isEmpty()) {
            String selectedSubject = requestDTO.getCategoryNames().get(0).trim();

            try {
                // Kiểm tra môn học hợp lệ theo bộ Enum hệ thống
                SubjectCode subject = SubjectCode.valueOf(selectedSubject.toUpperCase());
                selectedSubject = subject.name();

                // 🔍 Bước A: Tìm xem hệ thống ĐÃ CÓ môn học này chưa (Môn hệ thống có user_id IS NULL)
                String sqlCheckCategory = "SELECT category_id FROM document_categories WHERE UPPER(category_name) = ? AND user_id IS NULL LIMIT 1";
                List<?> existingIds = entityManager.createNativeQuery(sqlCheckCategory)
                        .setParameter(1, selectedSubject.toUpperCase())
                        .getResultList();

                java.util.UUID targetCategoryId;

                if (!existingIds.isEmpty()) {
                    // 👉 Nếu ĐÃ CÓ: Lấy luôn ID cũ để dùng chung
                    targetCategoryId = (java.util.UUID) existingIds.get(0);
                } else {
                    // 👉 Nếu CHƯA CÓ: Tạo mới dòng danh mục hệ thống
                    targetCategoryId = java.util.UUID.randomUUID();
                    String sqlInsertNewCategory = "INSERT INTO document_categories (category_id, document_id, category_name, category_type, created_at, user_id) " +
                            "VALUES (?, ?, ?, ?, ?, NULL)";
                    entityManager.createNativeQuery(sqlInsertNewCategory)
                            .setParameter(1, targetCategoryId)
                            .setParameter(2, savedDoc.getId()) // Gán tạm thời document_id đầu tiên kích hoạt môn này
                            .setParameter(3, selectedSubject)
                            .setParameter(4, "SUBJECT")
                            .setParameter(5, java.time.OffsetDateTime.now())
                            .executeUpdate();
                }

                // 🌟 Bước B: Cập nhật ngược lại khóa ngoại category_id cho Document vừa tạo để chúng ăn chung ID
                String sqlUpdateDoc = "UPDATE documents SET category_id = ? WHERE document_id = ?";
                entityManager.createNativeQuery(sqlUpdateDoc)
                        .setParameter(1, targetCategoryId)
                        .setParameter(2, savedDoc.getId())
                        .executeUpdate();

                // 🔥 ĐỒNG BỘ LÊN RAM: Gán luôn vào savedDoc để hàm mapToResponseDTO ở cuối không bị trả về null
                savedDoc.setCategoryId(targetCategoryId);

            } catch (IllegalArgumentException e) {
                // 🌟 XỬ LÝ MÔN TỰ NHẬP Ở MỤC "KHÁC" (Chỉ cá nhân người đó nhìn thấy)
                String customSubjectName = selectedSubject;

                // 🔍 Tìm xem chính User này đã từng tự tạo môn có tên này chưa
                String sqlCheckCustom = "SELECT category_id FROM document_categories WHERE UPPER(category_name) = ? AND user_id = ? LIMIT 1";
                List<?> existingCustomIds = entityManager.createNativeQuery(sqlCheckCustom)
                        .setParameter(1, customSubjectName.toUpperCase())
                        .setParameter(2, userId)
                        .getResultList();

                java.util.UUID targetCustomId;

                if (!existingCustomIds.isEmpty()) {
                    targetCustomId = (java.util.UUID) existingCustomIds.get(0);
                } else {
                    targetCustomId = java.util.UUID.randomUUID();
                    String sqlInsertCustom = "INSERT INTO document_categories (category_id, document_id, category_name, category_type, created_at, user_id) " +
                            "VALUES (?, ?, ?, ?, ?, ?)";
                    entityManager.createNativeQuery(sqlInsertCustom)
                            .setParameter(1, targetCustomId)
                            .setParameter(2, savedDoc.getId())
                            .setParameter(3, customSubjectName)
                            .setParameter(4, "CUSTOM_SUBJECT")
                            .setParameter(5, java.time.OffsetDateTime.now())
                            .setParameter(6, userId)
                            .executeUpdate();
                }

                // Cập nhật ngược lại khóa ngoại cho bảng documents
                String sqlUpdateDocCustom = "UPDATE documents SET category_id = ? WHERE document_id = ?";
                entityManager.createNativeQuery(sqlUpdateDocCustom)
                        .setParameter(1, targetCustomId)
                        .setParameter(2, savedDoc.getId())
                        .executeUpdate();

                // 🔥 ĐỒNG BỘ LÊN RAM: Gán luôn vào savedDoc để hàm mapToResponseDTO ở cuối không bị trả về null
                savedDoc.setCategoryId(targetCustomId);
            }

            entityManager.flush(); // Đẩy toàn bộ dữ liệu đồng bộ xuống database
        }

        storage.setUsedQuota(updatedUsedQuota);
        cloudStorageRepository.save(storage);

        storageUploadService.logSuccess(storage, requestDTO.getDocumentName(), actualFileSize);
        return mapToResponseDTO(savedDoc);
    }

    @Transactional(readOnly = true)
    @Override
    public List<DocumentResponseDTO> getAllDocumentsByUser() {
        Authentication au = SecurityContextHolder.getContext().getAuthentication();
        if (au == null || !au.isAuthenticated() || "anonymousUser".equals(au.getPrincipal().toString())) {
            throw new RuntimeException("You are not login yet!");
        }
        User user = userRepository.findByEmailIgnoreCase(au.getName())
                .orElseThrow(() -> new RuntimeException("This user is not found!"));

        List<Document> documents = documentRepository.findByUser(user);

        return documents.stream()
                .map(doc -> {
                    DocumentResponseDTO dto = new DocumentResponseDTO();
                    dto.setDocumentId(doc.getId());
                    dto.setDocumentName(doc.getDocumentName());
                    dto.setFileType(doc.getFileType());
                    dto.setPreviewUrl(doc.getPreviewUrl());
                    dto.setDownloadUrl(doc.getDownloadUrl());
                    dto.setCreatedAt(doc.getCreatedAt());
                    dto.setDescription(doc.getDescription());
                    dto.setIsPublic(doc.isPublic());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponseDTO getDocumentDetail(UUID documentId) {
        // 🌟 LẤY USER NGẦM TỪ TOKEN
        Authentication au = SecurityContextHolder.getContext().getAuthentication();
        if (au == null || !au.isAuthenticated() || "anonymousUser".equals(au.getPrincipal().toString())) {
            throw new RuntimeException("You are not login yet!");
        }
        User user = userRepository.findByEmailIgnoreCase(au.getName())
                .orElseThrow(() -> new RuntimeException("This user is not found!"));

        UUID userId = user.getId();

        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        boolean isOwner = doc.getUser() != null && Objects.equals(doc.getUser().getId(), userId);
        boolean isPublic = doc.isPublic();
        boolean isSharedWithMe = documentShareRepository.existsByDocument_IdAndSharedWithUser_Id(documentId, userId);

        if (!isOwner && !isPublic && !isSharedWithMe) {
            throw new RuntimeException("Bạn không có quyền xem tài liệu này");
        }

        return mapToResponseDTO(doc);
    }

    @Override
    @Transactional
    public DocumentResponseDTO updateDocumentName(UUID documentId, String newName) {
        // 🌟 LẤY USER NGẦM TỪ TOKEN
        Authentication au = SecurityContextHolder.getContext().getAuthentication();
        if (au == null || !au.isAuthenticated() || "anonymousUser".equals(au.getPrincipal().toString())) {
            throw new RuntimeException("You are not login yet!");
        }
        User user = userRepository.findByEmailIgnoreCase(au.getName())
                .orElseThrow(() -> new RuntimeException("This user is not found!"));

        UUID userId = user.getId();

        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        boolean isOwner = doc.getUser() != null && Objects.equals(doc.getUser().getId(), userId);

        java.util.Optional<DocumentShare> shareOpt = documentShareRepository.findByDocument_IdAndSharedWithUser_Id(documentId, userId);
        boolean hasEditPermission = shareOpt.isPresent() && "edit".equalsIgnoreCase(shareOpt.get().getPermissionType());

        if (!isOwner && !hasEditPermission) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa tài liệu này!");
        }

        doc.setDocumentName(newName);
        Document updatedDoc = documentRepository.save(doc);

        return mapToResponseDTO(updatedDoc);
    }

    @Override
    @Transactional
    public void deleteDocument(UUID documentId) {
        // 🌟 LẤY USER NGẦM TỪ TOKEN
        Authentication au = SecurityContextHolder.getContext().getAuthentication();
        if (au == null || !au.isAuthenticated() || "anonymousUser".equals(au.getPrincipal().toString())) {
            throw new RuntimeException("You are not login yet!");
        }
        User user = userRepository.findByEmailIgnoreCase(au.getName())
                .orElseThrow(() -> new RuntimeException("This user is not found!"));

        UUID userId = user.getId();

        Document document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Tài liệu không tồn tại hoặc bạn không có quyền xóa"));

        long actualFileSize = (document.getFileSize() != null) ? document.getFileSize() : 0L;

        try {
            String downloadUrl = document.getDownloadUrl();
            if (downloadUrl == null || downloadUrl.trim().isEmpty()) {
                downloadUrl = document.getPreviewUrl();
            }

            String fileKey = "";
            if (downloadUrl != null && downloadUrl.contains("/documents/")) {
                fileKey = downloadUrl.substring(downloadUrl.indexOf("/documents/") + 11);
            } else {
                String extension = document.getFileType().name().trim().toLowerCase();
                fileKey = userId.toString() + "/" + documentId.toString() + "." + extension;
            }

            String supabaseUrl = "https://ybgeblpkptrsefpafthb.supabase.co/storage/v1/object/documents/" + fileKey;

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            String supabaseToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InliZ2VibHBrcHRyc2VmcGFmdGhiIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc4MDYwNDM0MSwiZXhwIjoyMDk2MTgwMzQxfQ.T0MpOxyT3aFSdVMy-o4j22D8OxO9hXzRLzDjkbputUI";

            headers.set("Authorization", "Bearer " + supabaseToken);
            headers.set("apiKey", supabaseToken);

            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            restTemplate.exchange(supabaseUrl, org.springframework.http.HttpMethod.DELETE, entity, String.class);
            System.out.println("==> Đã xóa file vật lý thành công trên Supabase Bucket: " + fileKey);

        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String errorResponse = e.getResponseBodyAsString();
            if (e.getStatusCode().value() == 404 || errorResponse.contains("not_found")) {
                System.out.println("==> Cảnh báo: File không tìm thấy trên Cloud (404), tiến hành dọn dẹp tiếp Database.");
            } else {
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

    @Override
    @Transactional(readOnly = true)
    public Resource downloadDocumentFile(UUID documentId) {
        // 🌟 LẤY USER NGẦM TỪ TOKEN
        Authentication au = SecurityContextHolder.getContext().getAuthentication();
        if (au == null || !au.isAuthenticated() || "anonymousUser".equals(au.getPrincipal().toString())) {
            throw new RuntimeException("You are not login yet!");
        }
        User user = userRepository.findByEmailIgnoreCase(au.getName())
                .orElseThrow(() -> new RuntimeException("This user is not found!"));

        UUID userId = user.getId();

        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        boolean isOwner = doc.getUser() != null && java.util.Objects.equals(doc.getUser().getId(), userId);
        boolean isPublic = doc.isPublic();

        java.util.Optional<DocumentShare> shareOpt = documentShareRepository.findByDocument_IdAndSharedWithUser_Id(documentId, userId);

        boolean hasDownloadPermission = false;
        if (shareOpt.isPresent()) {
            String permission = shareOpt.get().getPermissionType();
            if ("download".equalsIgnoreCase(permission) || "edit".equalsIgnoreCase(permission)) {
                hasDownloadPermission = true;
            }
        }

        if (!isOwner && !isPublic && !hasDownloadPermission) {
            throw new RuntimeException("Tài liệu này chỉ cho phép xem trực tuyến, bạn không có quyền tải xuống!");
        }

        return fetchFileResourceFromCloud(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource getFileResourceForPreview(UUID documentId) {
        // 🌟 LẤY USER NGẦM TỪ TOKEN
        Authentication au = SecurityContextHolder.getContext().getAuthentication();
        if (au == null || !au.isAuthenticated() || "anonymousUser".equals(au.getPrincipal().toString())) {
            throw new RuntimeException("You are not login yet!");
        }
        User user = userRepository.findByEmailIgnoreCase(au.getName())
                .orElseThrow(() -> new RuntimeException("This user is not found!"));

        UUID userId = user.getId();

        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        boolean isOwner = doc.getUser() != null && java.util.Objects.equals(doc.getUser().getId(), userId);
        boolean isPublic = doc.isPublic();
        boolean isSharedWithMe = documentShareRepository.existsByDocument_IdAndSharedWithUser_Id(documentId, userId);

        if (!isOwner && !isPublic && !isSharedWithMe) {
            throw new RuntimeException("Bạn không có quyền xem trước tài liệu này!");
        }

        return fetchFileResourceFromCloud(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> searchDocumentsByFilter(String searchText, UUID categoryId) {
        // 🌟 LẤY USER NGẦM TỪ TOKEN
        Authentication au = SecurityContextHolder.getContext().getAuthentication();
        if (au == null || !au.isAuthenticated() || "anonymousUser".equals(au.getPrincipal().toString())) {
            throw new RuntimeException("You are not login yet!");
        }
        User user = userRepository.findByEmailIgnoreCase(au.getName())
                .orElseThrow(() -> new RuntimeException("This user is not found!"));

        List<Document> documents = documentRepository.searchDocumentsWithCategory(user.getId(), searchText != null ? searchText.trim() : null
        );

        return documents.stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public DocumentResponseDTO toggleDocumentPublicStatus(UUID documentId, boolean isPublic) {
        // 🌟 LẤY USER NGẦM TỪ TOKEN
        Authentication au = SecurityContextHolder.getContext().getAuthentication();
        if (au == null || !au.isAuthenticated() || "anonymousUser".equals(au.getPrincipal().toString())) {
            throw new RuntimeException("You are not login yet!");
        }
        User user = userRepository.findByEmailIgnoreCase(au.getName())
                .orElseThrow(() -> new RuntimeException("This user is not found!"));

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu yêu cầu."));

        if (!document.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa trạng thái của tài liệu này!");
        }

        document.setPublic(isPublic);
        Document updatedDoc = documentRepository.saveAndFlush(document);

        return mapToResponseDTO(updatedDoc);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> getPublicDocuments() {
        return documentRepository.findByIsPublicTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    public List<Document> getMyDocuments() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Người dùng chưa được xác thực hệ thống");
        }
        String currentUserEmail = authentication.getName();
        User user = userRepository.findByEmailIgnoreCase(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản: " + currentUserEmail));

        return documentRepository.findByUserId(user.getId());
    }

    private Resource fetchFileResourceFromCloud(Document doc) {
        try {
            String stringUrl = doc.getDownloadUrl();
            java.net.URL url = java.net.URI.create(stringUrl).toURL();
            try (java.io.InputStream inputStream = url.openStream()) {
                return new ByteArrayResource(inputStream.readAllBytes());
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
        dto.setDescription(document.getDescription());
        dto.setTextContent(document.getDescription());
        dto.setIsPublic(document.isPublic());
        return dto;
    }
}
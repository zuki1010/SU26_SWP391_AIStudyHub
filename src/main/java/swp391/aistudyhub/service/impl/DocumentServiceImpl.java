package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import swp391.aistudyhub.entity.DocumentShare;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.enums.Semester;
import swp391.aistudyhub.enums.SubjectCode;
import swp391.aistudyhub.repository.CloudStorageRepository;
import swp391.aistudyhub.repository.DocumentChunkRepository;
import swp391.aistudyhub.repository.DocumentRepository;
import swp391.aistudyhub.repository.DocumentShareRepository;
import swp391.aistudyhub.repository.UserRepository;
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

    @Value("${supabase.service-role-key:}")
    private String supabaseServiceRoleKey;

    @Override
    @Transactional
    public DocumentResponseDTO createDocument(DocumentRequestDTO requestDTO) {
        User user = getCurrentUser();
        UUID userId = user.getId();

        CloudStorage storage = cloudStorageRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu hình không gian lưu trữ của người dùng này."));

        long actualFileSize = requestDTO.getFileSize() != null ? requestDTO.getFileSize() : 0L;
        long updatedUsedQuota = storage.getUsedQuota() + actualFileSize;

        if (updatedUsedQuota > storage.getTotalQuota()) {
            storageUploadService.logFailure(
                    storage,
                    requestDTO.getDocumentName(),
                    actualFileSize,
                    "FAILED_QUOTA_FULL"
            );

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
        doc.setCategoryId(null);

        Document savedDoc = documentRepository.saveAndFlush(doc);

        handleDocumentCategories(savedDoc, userId, requestDTO.getCategoryNames());

        storage.setUsedQuota(updatedUsedQuota);
        cloudStorageRepository.save(storage);

        storageUploadService.logSuccess(storage, requestDTO.getDocumentName(), actualFileSize);

        return mapToResponseDTO(savedDoc);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> getAllDocumentsByUser() {
        User user = getCurrentUser();

        List<Document> documents = documentRepository.findByUser(user);

        return documents.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponseDTO getDocumentDetail(UUID documentId) {
        User user = getCurrentUser();
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
        User user = getCurrentUser();
        UUID userId = user.getId();

        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        boolean isOwner = doc.getUser() != null && Objects.equals(doc.getUser().getId(), userId);

        java.util.Optional<DocumentShare> shareOpt =
                documentShareRepository.findByDocument_IdAndSharedWithUser_Id(documentId, userId);

        boolean hasEditPermission = shareOpt.isPresent()
                && "edit".equalsIgnoreCase(shareOpt.get().getPermissionType());

        if (!isOwner && !hasEditPermission) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa tài liệu này!");
        }

        if (newName == null || newName.trim().isEmpty()) {
            throw new RuntimeException("Tên tài liệu không được để trống!");
        }

        doc.setDocumentName(newName.trim());

        Document updatedDoc = documentRepository.save(doc);

        return mapToResponseDTO(updatedDoc);
    }

    @Override
    @Transactional
    public void deleteDocument(UUID documentId) {
        User user = getCurrentUser();
        UUID userId = user.getId();

        Document document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Tài liệu không tồn tại hoặc bạn không có quyền xóa"));

        long actualFileSize = document.getFileSize() != null ? document.getFileSize() : 0L;

        deletePhysicalFileFromSupabase(document, userId, documentId);

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
        User user = getCurrentUser();
        UUID userId = user.getId();

        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        boolean isOwner = doc.getUser() != null && Objects.equals(doc.getUser().getId(), userId);
        boolean isPublic = doc.isPublic();

        java.util.Optional<DocumentShare> shareOpt =
                documentShareRepository.findByDocument_IdAndSharedWithUser_Id(documentId, userId);

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
        User user = getCurrentUser();
        UUID userId = user.getId();

        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        boolean isOwner = doc.getUser() != null && Objects.equals(doc.getUser().getId(), userId);
        boolean isPublic = doc.isPublic();
        boolean isSharedWithMe = documentShareRepository.existsByDocument_IdAndSharedWithUser_Id(documentId, userId);

        if (!isOwner && !isPublic && !isSharedWithMe) {
            throw new RuntimeException("Bạn không có quyền xem trước tài liệu này!");
        }

        return fetchFileResourceFromCloud(doc);
    }

    @Override
@Transactional(readOnly = true)
public List<DocumentResponseDTO> searchDocumentsByFilter(String searchText) {
    User user = getCurrentUser();

    /*
     * Không truyền null xuống PostgreSQL.
     * Nếu truyền null vào query có điều kiện :searchText IS NULL,
     * PostgreSQL có thể báo:
     * could not determine data type of parameter
     */
    String cleanSearchText =
            searchText != null
                    ? searchText.trim()
                    : "";

    List<Document> documents = documentRepository.searchSmartAccessibleDocuments(
            user.getId(),
            cleanSearchText
    );

    return documents.stream()
            .map(this::mapToResponseDTO)
            .toList();
}

    @Override
    @Transactional
    public DocumentResponseDTO toggleDocumentPublicStatus(UUID documentId, boolean isPublic) {
        User user = getCurrentUser();

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu yêu cầu."));

        if (document.getUser() == null || !document.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa trạng thái của tài liệu này!");
        }

        if (isPublic) {
            /*
             * Khi user muốn public, không set public ngay.
             * Chuyển sang PENDING để Admin/Moderator duyệt.
             */
            document.setPublic(false);
            document.setStatus("PENDING");
        } else {
            /*
             * Khi user rút về private, cho về private ngay.
             */
            document.setPublic(false);
            document.setStatus("DEFAULT");
            document.setApprovedBy(null);
        }

        Document updatedDoc = documentRepository.saveAndFlush(document);

        return mapToResponseDTO(updatedDoc);
    }

    @Override
    @Transactional
    public DocumentResponseDTO approvePublicRequest(UUID documentId, String decision) {
        Authentication authentication = getAuthentication();

        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_MODERATOR")
                                || authority.getAuthority().equals("MODERATOR")
                                || authority.getAuthority().equals("ROLE_ADMIN")
                                || authority.getAuthority().equals("ADMIN")
                );

        if (!isStaff) {
            throw new RuntimeException("Bạn không có quyền thực hiện thao tác duyệt này!");
        }

        User reviewer = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin người duyệt trên hệ thống!"));

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu yêu cầu phê duyệt."));

        if (!"PENDING".equalsIgnoreCase(document.getStatus())) {
            throw new RuntimeException("Tài liệu này hiện không có yêu cầu phê duyệt nào cần xử lý hoặc đã được duyệt trước đó!");
        }

        if ("ACCEPT".equalsIgnoreCase(decision)) {
            document.setPublic(true);
            document.setStatus("SUCCESS");
        } else if ("DENY".equalsIgnoreCase(decision)) {
            document.setPublic(false);
            document.setStatus("DEFAULT");
        } else {
            throw new RuntimeException("Quyết định phê duyệt không hợp lệ! Chỉ chấp nhận 'ACCEPT' hoặc 'DENY'.");
        }

        document.setApprovedBy(reviewer);

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
        User user = getCurrentUser();

        return documentRepository.findByUserId(user.getId());
    }

    private Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
            throw new RuntimeException("You are not login yet!");
        }

        return authentication;
    }

    private User getCurrentUser() {
        Authentication authentication = getAuthentication();

        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new RuntimeException("This user is not found!"));
    }

    private void handleDocumentCategories(
            Document savedDoc,
            UUID userId,
            List<String> categoryNames
    ) {
        if (categoryNames == null || categoryNames.isEmpty()) {
            return;
        }

        String selectedSubject = categoryNames.get(0);

        if (selectedSubject == null || selectedSubject.trim().isEmpty()) {
            return;
        }

        selectedSubject = selectedSubject.trim();

        try {
            SubjectCode subject = SubjectCode.valueOf(selectedSubject.toUpperCase());
            selectedSubject = subject.name();

            Semester semester = subject.getSemester();

            UUID semesterCategoryId = findOrCreateSemesterCategory(savedDoc.getId(), semester);
            UUID subjectCategoryId = findOrCreateSubjectCategory(
                    savedDoc.getId(),
                    selectedSubject,
                    semesterCategoryId
            );

            updateDocumentCategory(savedDoc, subjectCategoryId);
        } catch (IllegalArgumentException e) {
            UUID customCategoryId = findOrCreateCustomCategory(
                    savedDoc.getId(),
                    userId,
                    selectedSubject
            );

            updateDocumentCategory(savedDoc, customCategoryId);
        }

        entityManager.flush();
    }

    private UUID findOrCreateSemesterCategory(UUID documentId, Semester semester) {
        String sqlCheckSemester =
                "SELECT category_id FROM document_categories " +
                        "WHERE UPPER(category_name) = ? AND user_id IS NULL LIMIT 1";

        List<?> existingSemesterIds = entityManager.createNativeQuery(sqlCheckSemester)
                .setParameter(1, semester.name().toUpperCase())
                .getResultList();

        if (!existingSemesterIds.isEmpty()) {
            return (UUID) existingSemesterIds.get(0);
        }

        UUID semesterCategoryId = UUID.randomUUID();

        String sqlInsertSemester =
                "INSERT INTO document_categories " +
                        "(category_id, document_id, category_name, category_type, created_at, parent_id, user_id) " +
                        "VALUES (?, ?, ?, ?, ?, NULL, NULL)";

        entityManager.createNativeQuery(sqlInsertSemester)
                .setParameter(1, semesterCategoryId)
                .setParameter(2, documentId)
                .setParameter(3, semester.name())
                .setParameter(4, "SEMESTER")
                .setParameter(5, java.time.OffsetDateTime.now())
                .executeUpdate();

        return semesterCategoryId;
    }

    private UUID findOrCreateSubjectCategory(
            UUID documentId,
            String selectedSubject,
            UUID semesterCategoryId
    ) {
        String sqlCheckSubject =
                "SELECT category_id FROM document_categories " +
                        "WHERE UPPER(category_name) = ? AND parent_id = ? AND user_id IS NULL LIMIT 1";

        List<?> existingSubjectIds = entityManager.createNativeQuery(sqlCheckSubject)
                .setParameter(1, selectedSubject.toUpperCase())
                .setParameter(2, semesterCategoryId)
                .getResultList();

        if (!existingSubjectIds.isEmpty()) {
            return (UUID) existingSubjectIds.get(0);
        }

        UUID targetCategoryId = UUID.randomUUID();

        String sqlInsertSubject =
                "INSERT INTO document_categories " +
                        "(category_id, document_id, category_name, category_type, created_at, parent_id, user_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NULL)";

        entityManager.createNativeQuery(sqlInsertSubject)
                .setParameter(1, targetCategoryId)
                .setParameter(2, documentId)
                .setParameter(3, selectedSubject)
                .setParameter(4, "SUBJECT")
                .setParameter(5, java.time.OffsetDateTime.now())
                .setParameter(6, semesterCategoryId)
                .executeUpdate();

        return targetCategoryId;
    }

    private UUID findOrCreateCustomCategory(
            UUID documentId,
            UUID userId,
            String customSubjectName
    ) {
        String sqlCheckCustom =
                "SELECT category_id FROM document_categories " +
                        "WHERE UPPER(category_name) = ? AND user_id = ? LIMIT 1";

        List<?> existingCustomIds = entityManager.createNativeQuery(sqlCheckCustom)
                .setParameter(1, customSubjectName.toUpperCase())
                .setParameter(2, userId)
                .getResultList();

        if (!existingCustomIds.isEmpty()) {
            return (UUID) existingCustomIds.get(0);
        }

        UUID targetCustomId = UUID.randomUUID();

        String sqlInsertCustom =
                "INSERT INTO document_categories " +
                        "(category_id, document_id, category_name, category_type, created_at, parent_id, user_id) " +
                        "VALUES (?, ?, ?, ?, ?, NULL, ?)";

        entityManager.createNativeQuery(sqlInsertCustom)
                .setParameter(1, targetCustomId)
                .setParameter(2, documentId)
                .setParameter(3, customSubjectName)
                .setParameter(4, "CUSTOM_SUBJECT")
                .setParameter(5, java.time.OffsetDateTime.now())
                .setParameter(6, userId)
                .executeUpdate();

        return targetCustomId;
    }

    private void updateDocumentCategory(Document savedDoc, UUID categoryId) {
        String sqlUpdateDoc =
                "UPDATE documents SET category_id = ? WHERE document_id = ?";

        entityManager.createNativeQuery(sqlUpdateDoc)
                .setParameter(1, categoryId)
                .setParameter(2, savedDoc.getId())
                .executeUpdate();

        savedDoc.setCategoryId(categoryId);
    }

    private void deletePhysicalFileFromSupabase(
            Document document,
            UUID userId,
            UUID documentId
    ) {
        try {
            String downloadUrl = document.getDownloadUrl();

            if (downloadUrl == null || downloadUrl.trim().isEmpty()) {
                downloadUrl = document.getPreviewUrl();
            }

            String fileKey;

            if (downloadUrl != null && downloadUrl.contains("/documents/")) {
                fileKey = downloadUrl.substring(downloadUrl.indexOf("/documents/") + 11);
            } else {
                String extension =
                        document.getFileType() != null
                                ? document.getFileType().name().trim().toLowerCase()
                                : "file";

                fileKey = userId + "/" + documentId + "." + extension;
            }

            if (supabaseServiceRoleKey == null || supabaseServiceRoleKey.isBlank()) {
                throw new RuntimeException("Thiếu cấu hình supabase.service-role-key để xóa file vật lý.");
            }

            String supabaseUrl =
                    "https://ybgeblpkptrsefpafthb.supabase.co/storage/v1/object/documents/" + fileKey;

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseServiceRoleKey);
            headers.set("apiKey", supabaseServiceRoleKey);

            org.springframework.http.HttpEntity<String> entity =
                    new org.springframework.http.HttpEntity<>(headers);

            restTemplate.exchange(
                    supabaseUrl,
                    org.springframework.http.HttpMethod.DELETE,
                    entity,
                    String.class
            );

            System.out.println("==> Đã xóa file vật lý thành công trên Supabase Bucket: " + fileKey);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String errorResponse = e.getResponseBodyAsString();

            if (e.getStatusCode().value() == 404 || errorResponse.contains("not_found")) {
                System.out.println("==> Cảnh báo: File không tìm thấy trên Cloud, tiếp tục dọn Database.");
            } else {
                throw new RuntimeException(
                        "Không thể xóa file vật lý trên Supabase Bucket. Mã lỗi: "
                                + e.getStatusCode()
                                + " - Chi tiết: "
                                + errorResponse
                );
            }
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";

            if (msg.contains("404") || msg.contains("not_found")) {
                System.out.println("==> Cảnh báo: Phát hiện mã lỗi 404, tiếp tục dọn Database.");
            } else {
                throw new RuntimeException("Lỗi kết nối mạng đến Supabase: " + e.getMessage());
            }
        }
    }

    private Resource fetchFileResourceFromCloud(Document doc) {
        try {
            String stringUrl = doc.getDownloadUrl();

            if (stringUrl == null || stringUrl.trim().isEmpty()) {
                stringUrl = doc.getPreviewUrl();
            }

            if (stringUrl == null || stringUrl.trim().isEmpty()) {
                throw new RuntimeException("Tài liệu chưa có đường dẫn file.");
            }

            java.net.URL url = java.net.URI.create(stringUrl).toURL();

            try (InputStream inputStream = url.openStream()) {
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
        dto.setFileSize(document.getFileSize());
        dto.setPreviewUrl(document.getPreviewUrl());
        dto.setDownloadUrl(document.getDownloadUrl());
        dto.setCreatedAt(document.getCreatedAt());
        dto.setDescription(document.getDescription());
        dto.setTextContent(document.getDescription());
        dto.setIsPublic(document.isPublic());
        dto.setStatus(document.getStatus());

        return dto;
    }
}
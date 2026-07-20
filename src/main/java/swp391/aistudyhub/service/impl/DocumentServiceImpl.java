package swp391.aistudyhub.service.impl;

import jakarta.persistence.EntityManager;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
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
import swp391.aistudyhub.entity.*;
import swp391.aistudyhub.enums.*;
import swp391.aistudyhub.repository.*;
import swp391.aistudyhub.service.DocumentChunkService;
import swp391.aistudyhub.service.DocumentService;
import swp391.aistudyhub.service.StorageUploadService;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

    @Autowired
    private DocumentChunkService documentChunkService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private StorageUploadService storageUploadService;

    @Autowired
    private DocumentShareRepository documentShareRepository;

    @Value("${supabase.url:https://ybgeblpkptrsefpafthb.supabase.co}")
    private String supabaseUrl;

    @Value("${supabase.bucket-name:documents}")
    private String bucketName;

    @Value("${supabase.service-role-key:}")
    private String supabaseServiceRoleKey;

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Override
    @Transactional
    public DocumentResponseDTO createDocument(DocumentRequestDTO requestDTO) {
        User user = getCurrentUser();
        UUID userId = user.getId();

        if (requestDTO.getSubjectCode() == null) {
            throw new RuntimeException("Vui lòng chọn môn học hợp lệ!");
        }

        CloudStorage storage = cloudStorageRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu hình không gian lưu trữ của người dùng này."));

        long actualFileSize = requestDTO.getFileSize() != null ? requestDTO.getFileSize() : 0L;
        long usedQuota = storage.getUsedQuota() != null ? storage.getUsedQuota() : 0L;
        long totalQuota = storage.getTotalQuota() != null ? storage.getTotalQuota() : 0L;
        long updatedUsedQuota = usedQuota + actualFileSize;

        if (updatedUsedQuota > totalQuota) {
            storageUploadService.logFailure(
                    storage,
                    requestDTO.getDocumentName(),
                    actualFileSize,
                    "FAILED_QUOTA_FULL"
            );

            throw new RuntimeException("Không gian lưu trữ đám mây của bạn đã đầy!");
        }

        SystemConfig systemConfig = systemConfigRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("This config is not available"));

        Document document = new Document();
        document.setUser(user);
        document.setDocumentName(requestDTO.getDocumentName());
        document.setFileType(requestDTO.getFileType());
        document.setPreviewUrl(requestDTO.getPreviewUrl());
        document.setDownloadUrl(requestDTO.getDownloadUrl());
        if(requestDTO.getFileSize() > systemConfig.getMaxFileSizeMb()) {
            throw new IllegalArgumentException("Maximum size is" + systemConfig.getMaxFileSizeMb());
        } else {
            document.setFileSize(requestDTO.getFileSize());
        }
        document.setDescription(requestDTO.getDescription());
        document.setStatus(
                requestDTO.getStatus() != null
                        ? requestDTO.getStatus()
                        : StatusPublicDoc.DEFAULT
        );
        document.setPublic(false);
        document.setCategoryId(null);

        Document savedDocument = documentRepository.saveAndFlush(document);

        UUID subjectCategoryId = handleDocumentCategories(
                savedDocument,
                requestDTO.getSubjectCode()
        );

        savedDocument.setCategoryId(subjectCategoryId);
        updateDocumentCategory(savedDocument, subjectCategoryId);

        savedDocument = documentRepository.saveAndFlush(savedDocument);

        storage.setUsedQuota(updatedUsedQuota);
        cloudStorageRepository.saveAndFlush(storage);

        storageUploadService.logSuccess(storage, requestDTO.getDocumentName(), actualFileSize);

        try {
            String fileUrl = savedDocument.getDownloadUrl() != null && !savedDocument.getDownloadUrl().isBlank()
                    ? savedDocument.getDownloadUrl()
                    : savedDocument.getPreviewUrl();

            // Truyền savedDocument.getFileType()
            String fullTextContent = extractTextFromUrl(fileUrl, savedDocument.getFileType());

            if (fullTextContent != null && !fullTextContent.trim().isEmpty()) {
                System.out.println("==> RAG LOG: Trích xuất thành công " + fullTextContent.length() + " ký tự chữ từ file.");

                // SỬA ĐÂY: Dùng biến documentChunkService (chữ d thường) đã @Autowired
                documentChunkService.chunkAndEmbedDocument(savedDocument, fullTextContent);
            } else {
                System.out.println("==> RAG WARNING: File rỗng hoặc không thể trích xuất chữ từ URL: " + fileUrl);
            }
        } catch (Exception e) {
            System.err.println("==> RAG ERROR: Lỗi trong quá trình đọc file và băm Chunk: " + e.getMessage());
        }


        return mapToResponseDTO(savedDocument);
    }

    private String extractTextFromUrl(String fileUrl, FileType fileType) {
        if (fileUrl == null || fileUrl.isBlank()) return "";

        // Chuyển kiểu Enum thành String để so sánh
        String typeStr = fileType != null ? fileType.name().toLowerCase() : "";

        try {
            java.net.URL url = java.net.URI.create(fileUrl).toURL();

            // 1. Trường hợp File Văn Bản Thường (.txt)
            if ("txt".equalsIgnoreCase(typeStr) || fileUrl.toLowerCase().endsWith(".txt")) {
                try (InputStream in = url.openStream()) {
                    return new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
            }

            // 2. Trường hợp File PDF (.pdf)
            if ("pdf".equalsIgnoreCase(typeStr) || fileUrl.toLowerCase().endsWith(".pdf")) {
                try (InputStream in = url.openStream();
                     PDDocument pdfDocument = PDDocument.load(in)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    return stripper.getText(pdfDocument);
                }
            }
        } catch (Exception e) {
            System.err.println("==> LỖI BÓC TÁCH CHỮ TỪ URL SUPABASE: " + e.getMessage());
        }
        return "";
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> getAllDocumentsByUser() {
        User user = getCurrentUser();

        return documentRepository.findByUser(user)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponseDTO getDocumentDetail(UUID documentId) {
        User user = getCurrentUser();
        UUID userId = user.getId();

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        boolean isOwner = document.getUser() != null
                && Objects.equals(document.getUser().getId(), userId);

        boolean isPublic = document.isPublic();

        boolean isSharedWithMe =
                documentShareRepository.existsByDocument_IdAndSharedWithUser_Id(documentId, userId);

        if (!isOwner && !isPublic && !isSharedWithMe) {
            throw new RuntimeException("Bạn không có quyền xem tài liệu này");
        }

        return mapToResponseDTO(document);
    }

    @Override
    @Transactional
    public DocumentResponseDTO updateDocumentName(UUID documentId, String newName) {
        User user = getCurrentUser();
        UUID userId = user.getId();

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        boolean isOwner = document.getUser() != null
                && Objects.equals(document.getUser().getId(), userId);

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

        document.setDocumentName(newName.trim());

        Document updatedDocument = documentRepository.saveAndFlush(document);

        return mapToResponseDTO(updatedDocument);
    }

    @Override
    @Transactional
    public void deleteDocument(UUID documentId) {
        User user = getCurrentUser();
        UUID userId = user.getId();

        Document document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Tài liệu không tồn tại hoặc bạn không có quyền xóa"));

        long actualFileSize = document.getFileSize() != null ? document.getFileSize() : 0L;

        /*
         * Xóa file vật lý qua Supabase Storage REST API.
         * Tự động catch lỗi nếu sai Token/Key để đảm bảo DB vẫn được dọn sạch.
         */
        deletePhysicalFileFromSupabase(document, userId, documentId);

        CloudStorage storage = cloudStorageRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Cấu hình lưu trữ đám mây không tồn tại"));

        long usedQuota = storage.getUsedQuota() != null ? storage.getUsedQuota() : 0L;
        long newUsedQuota = Math.max(0, usedQuota - actualFileSize);

        storage.setUsedQuota(newUsedQuota);
        cloudStorageRepository.saveAndFlush(storage);

        documentChunkRepository.deleteByDocument_Id(documentId);
        documentRepository.delete(document);
        documentRepository.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadDocumentFile(UUID documentId) {
        User user = getCurrentUser();
        UUID userId = user.getId();

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        boolean isOwner = document.getUser() != null
                && Objects.equals(document.getUser().getId(), userId);

        boolean isPublic = document.isPublic();

        java.util.Optional<DocumentShare> shareOpt =
                documentShareRepository.findByDocument_IdAndSharedWithUser_Id(documentId, userId);

        boolean hasDownloadPermission = shareOpt.isPresent()
                && (
                "download".equalsIgnoreCase(shareOpt.get().getPermissionType())
                        || "edit".equalsIgnoreCase(shareOpt.get().getPermissionType())
        );

        if (!isOwner && !isPublic && !hasDownloadPermission) {
            throw new RuntimeException("Tài liệu này chỉ cho phép xem trực tuyến, bạn không có quyền tải xuống!");
        }

        return fetchFileResourceFromCloud(document);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource getFileResourceForPreview(UUID documentId) {
        User user = getCurrentUser();
        UUID userId = user.getId();

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        boolean isOwner = document.getUser() != null
                && Objects.equals(document.getUser().getId(), userId);

        boolean isPublic = document.isPublic();

        boolean isSharedWithMe =
                documentShareRepository.existsByDocument_IdAndSharedWithUser_Id(documentId, userId);

        if (!isOwner && !isPublic && !isSharedWithMe) {
            throw new RuntimeException("Bạn không có quyền xem trước tài liệu này!");
        }

        return fetchFileResourceFromCloud(document);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> searchDocumentsByFilter(String searchText) {
        User user = getCurrentUser();

        String cleanSearchText = searchText != null ? searchText.trim() : "";

        return documentRepository.searchSmartAccessibleDocuments(
                        user.getId(),
                        cleanSearchText
                )
                .stream()
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
            document.setPublic(false);
            document.setStatus(StatusPublicDoc.PENDING);
        } else {
            document.setPublic(false);
            document.setStatus(StatusPublicDoc.DEFAULT);
            document.setApprovedBy(null);
        }

        Document updatedDocument = documentRepository.saveAndFlush(document);

        return mapToResponseDTO(updatedDocument);
    }

    @Override
    @Transactional
    public DocumentResponseDTO approvePublicRequest(UUID documentId, RequestPublicDoc decision) {
        if (decision == null) {
            throw new RuntimeException("Quyết định phê duyệt không hợp lệ!");
        }

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

        if (document.getStatus() != StatusPublicDoc.PENDING) {
            throw new RuntimeException("Tài liệu này hiện không có yêu cầu phê duyệt nào cần xử lý hoặc đã được duyệt trước đó!");
        }

        if (decision == RequestPublicDoc.ACCEPT) {
            document.setPublic(true);
            document.setStatus(StatusPublicDoc.SUCCESS);
        } else if (decision == RequestPublicDoc.DENY) {
            document.setPublic(false);
            document.setStatus(StatusPublicDoc.DEFAULT);
        } else {
            throw new RuntimeException("Quyết định phê duyệt không hợp lệ! Chỉ chấp nhận ACCEPT hoặc DENY.");
        }

        document.setApprovedBy(reviewer);

        Document updatedDocument = documentRepository.saveAndFlush(document);

        return mapToResponseDTO(updatedDocument);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> getPublicDocuments() {
        return documentRepository.findByIsPublicTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
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

    private UUID handleDocumentCategories(
            Document savedDocument,
            SubjectCode subjectCode
    ) {
        if (subjectCode == null) {
            throw new RuntimeException("Vui lòng chọn một môn học hợp lệ từ danh sách hệ thống!");
        }

        Semester semester = subjectCode.getSemester();

        if (semester == null) {
            throw new RuntimeException("Môn học được chọn không thuộc bất kỳ học kỳ nào hiện tại!");
        }

        UUID semesterCategoryId = findOrCreateSemesterCategory(
                savedDocument.getId(),
                semester
        );

        UUID subjectCategoryId = findOrCreateSubjectCategory(
                savedDocument.getId(),
                subjectCode.name(),
                semesterCategoryId
        );

        entityManager.flush();

        return subjectCategoryId;
    }

    private UUID findOrCreateSemesterCategory(UUID documentId, Semester semester) {
        String sqlCheckSemester =
                "SELECT category_id FROM document_categories " +
                        "WHERE UPPER(category_name) = ? AND user_id IS NULL LIMIT 1";

        List<?> existingSemesterIds = entityManager.createNativeQuery(sqlCheckSemester)
                .setParameter(1, semester.name().toUpperCase())
                .getResultList();

        if (!existingSemesterIds.isEmpty()) {
            return toUuid(existingSemesterIds.get(0));
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
            String subjectName,
            UUID semesterCategoryId
    ) {
        String sqlCheckSubject =
                "SELECT category_id FROM document_categories " +
                        "WHERE UPPER(category_name) = ? AND parent_id = ? AND user_id IS NULL LIMIT 1";

        List<?> existingSubjectIds = entityManager.createNativeQuery(sqlCheckSubject)
                .setParameter(1, subjectName.toUpperCase())
                .setParameter(2, semesterCategoryId)
                .getResultList();

        if (!existingSubjectIds.isEmpty()) {
            return toUuid(existingSubjectIds.get(0));
        }

        UUID subjectCategoryId = UUID.randomUUID();

        String sqlInsertSubject =
                "INSERT INTO document_categories " +
                        "(category_id, document_id, category_name, category_type, created_at, parent_id, user_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NULL)";

        entityManager.createNativeQuery(sqlInsertSubject)
                .setParameter(1, subjectCategoryId)
                .setParameter(2, documentId)
                .setParameter(3, subjectName)
                .setParameter(4, "SUBJECT")
                .setParameter(5, java.time.OffsetDateTime.now())
                .setParameter(6, semesterCategoryId)
                .executeUpdate();

        return subjectCategoryId;
    }

    private void updateDocumentCategory(Document savedDocument, UUID categoryId) {
        String sqlUpdateDocument =
                "UPDATE documents SET category_id = ? WHERE document_id = ?";

        entityManager.createNativeQuery(sqlUpdateDocument)
                .setParameter(1, categoryId)
                .setParameter(2, savedDocument.getId())
                .executeUpdate();

        savedDocument.setCategoryId(categoryId);
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

            String fileKey = resolveSupabaseFileKey(downloadUrl);
            String serviceKey = normalizeSupabaseServiceKey(supabaseServiceRoleKey);

            if (fileKey == null || fileKey.isBlank()) {
                System.out.println("==> Cảnh báo: Không thể giải mã File Key từ url. FileUrl=" + downloadUrl);
                return;
            }

            // Bỏ kiểm tra dấu chấm (.) để chấp nhận cả chuỗi token thế hệ mới sb_secret_...
            if (serviceKey == null || serviceKey.isBlank()) {
                System.out.println("==> Cảnh báo: supabase.service-role-key trống, bỏ qua xóa file vật lý. FileKey=" + fileKey);
                return;
            }

            // Chuẩn hóa loại bỏ dấu gạch chéo dư thừa ở đầu key
            if (fileKey.startsWith("/")) {
                fileKey = fileKey.substring(1);
            }

            String baseUrl = supabaseUrl.endsWith("/")
                    ? supabaseUrl.substring(0, supabaseUrl.length() - 1)
                    : supabaseUrl;

            // ĐỔI ENDPOINT: Ghép trực tiếp bucket và fileKey vào URL theo chuẩn REST API đơn lẻ của Supabase
            String deleteUrl = baseUrl + "/storage/v1/object/" + bucketName + "/" + fileKey;

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + serviceKey);
            headers.set("apikey", serviceKey); // Gửi kèm api key song song để tránh lỗi phân quyền RLS

            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);

            // Sử dụng HTTP Method DELETE trực tiếp lên URL của file
            restTemplate.exchange(
                    deleteUrl,
                    org.springframework.http.HttpMethod.DELETE,
                    entity,
                    String.class
            );

            System.out.println("==> Đã gửi lệnh REST API xóa file vật lý thành công trên Supabase Bucket: " + fileKey);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String errorResponse = e.getResponseBodyAsString();

            if (e.getStatusCode().value() == 401
                    || e.getStatusCode().value() == 403
                    || e.getStatusCode().value() == 404
                    || errorResponse.contains("not_found")
                    || errorResponse.contains("Invalid Compact JWS")) {
                System.out.println("==> Cảnh báo: Không xóa được file vật lý trên Supabase, tiếp tục dọn Database.");
                System.out.println("==> Supabase status: " + e.getStatusCode());
                System.out.println("==> Supabase response: " + errorResponse);
                return;
            }

            throw new RuntimeException(
                    "Không thể xóa file vật lý trên Supabase Bucket. Mã lỗi: "
                            + e.getStatusCode()
                            + " - Chi tiết: "
                            + errorResponse
            );
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : "";

            if (message.contains("401")
                    || message.contains("403")
                    || message.contains("404")
                    || message.contains("not_found")) {
                System.out.println("==> Cảnh báo: Gặp lỗi xác thực/đường dẫn từ Supabase, tiếp tục dọn Database.");
                System.out.println("==> Lý do: " + message);
                return;
            }

            throw new RuntimeException("Lỗi kết nối Supabase: " + e.getMessage());
        }
    }

    private String resolveSupabaseFileKey(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return null;
        }
        // Ví dụ: .../storage/v1/object/public/your-bucket-name/userId/filename.pdf
        // Hoặc: .../storage/v1/object/public/your-bucket-name/filename.pdf
        String target = "/" + bucketName + "/";
        int index = fileUrl.indexOf(target);
        if (index != -1) {
            // Cắt toàn bộ chuỗi đứng sau tên bucket để làm File Key chuẩn xác trên Supabase
            return fileUrl.substring(index + target.length());
        }
        return null;
    }

    private String normalizeSupabaseServiceKey(String rawKey) {
        if (rawKey == null) {
            return "";
        }

        String key = rawKey.trim();

        if (key.startsWith("Bearer ")) {
            key = key.substring(7).trim();
        }

        return key.replace("\"", "")
                .replace("'", "")
                .trim();
    }

    private Resource fetchFileResourceFromCloud(Document document) {
        try {
            String fileUrl = document.getDownloadUrl();

            if (fileUrl == null || fileUrl.trim().isEmpty()) {
                fileUrl = document.getPreviewUrl();
            }

            if (fileUrl == null || fileUrl.trim().isEmpty()) {
                throw new RuntimeException("Tài liệu chưa có đường dẫn file.");
            }

            java.net.URL url = java.net.URI.create(fileUrl).toURL();

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
        dto.setIsPublic(document.isPublic());
        dto.setStatus(
                document.getStatus() != null
                        ? document.getStatus()
                        : StatusPublicDoc.DEFAULT
        );
        dto.setSubjectCode(resolveSubjectCodeFromDocument(document));

        return dto;
    }

    private SubjectCode resolveSubjectCodeFromDocument(Document document) {
        if (document.getCategoryId() == null) {
            return null;
        }

        try {
            String sqlGetCategoryName =
                    "SELECT category_name FROM document_categories WHERE category_id = ? LIMIT 1";

            Object result = entityManager.createNativeQuery(sqlGetCategoryName)
                    .setParameter(1, document.getCategoryId())
                    .getSingleResult();

            if (result == null) {
                return null;
            }

            String categoryName = String.valueOf(result).trim().toUpperCase();

            if (categoryName.isEmpty()) {
                return null;
            }

            return SubjectCode.valueOf(categoryName);
        } catch (IllegalArgumentException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private UUID toUuid(Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }

        return UUID.fromString(String.valueOf(value));
    }
}
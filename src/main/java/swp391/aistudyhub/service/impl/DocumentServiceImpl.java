package swp391.aistudyhub.service.impl;

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
import swp391.aistudyhub.dto.request.DocumentUpdateRequestDTO;
import swp391.aistudyhub.dto.response.DocumentResponseDTO;
import swp391.aistudyhub.entity.CloudStorage;
import swp391.aistudyhub.entity.Document;
import swp391.aistudyhub.entity.DocumentCategory;
import swp391.aistudyhub.entity.DocumentShare;
import swp391.aistudyhub.entity.SubscriptionPlan;
import swp391.aistudyhub.entity.SystemConfig;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.entity.UserMemberSubscription;
import swp391.aistudyhub.enums.FileType;
import swp391.aistudyhub.enums.MemberStatus;
import swp391.aistudyhub.enums.RequestPublicDoc;
import swp391.aistudyhub.enums.StatusPublicDoc;
import swp391.aistudyhub.repository.CloudStorageRepository;
import swp391.aistudyhub.repository.DocumentCategoryRepository;
import swp391.aistudyhub.repository.DocumentChunkRepository;
import swp391.aistudyhub.repository.DocumentRepository;
import swp391.aistudyhub.repository.DocumentShareRepository;
import swp391.aistudyhub.repository.SubscriptionPlanRepository;
import swp391.aistudyhub.repository.SystemConfigRepository;
import swp391.aistudyhub.repository.UserMemberSubscriptionRepository;
import swp391.aistudyhub.repository.UserRepository;
import swp391.aistudyhub.service.DocumentChunkService;
import swp391.aistudyhub.service.DocumentService;
import swp391.aistudyhub.service.MailService;
import swp391.aistudyhub.service.StorageUploadService;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
public class DocumentServiceImpl implements DocumentService {

    private static final double BYTES_PER_GB = 1073741824.0;
    private static final double BYTES_PER_MB = 1048576.0;

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
    private RestTemplate restTemplate;

    @Autowired
    private StorageUploadService storageUploadService;

    @Autowired
    private MailService mailService;

    @Autowired
    private DocumentShareRepository documentShareRepository;

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private UserMemberSubscriptionRepository userMemberSubscriptionRepository;

    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Autowired
    private DocumentCategoryRepository documentCategoryRepository;

    @Value("${supabase.url:https://ybgeblpkptrsefpafthb.supabase.co}")
    private String supabaseUrl;

    @Value("${supabase.bucket-name:documents}")
    private String bucketName;

    @Value("${supabase.service-role-key:}")
    private String supabaseServiceRoleKey;

    @Override
    @Transactional
    public DocumentResponseDTO createDocument(DocumentRequestDTO requestDTO) {
        User user = getCurrentUser();
        UUID userId = user.getId();

        if (requestDTO.getCategoryId() == null) {
            throw new RuntimeException("Vui lòng chọn môn học hợp lệ.");
        }

        DocumentCategory category = documentCategoryRepository.findById(requestDTO.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Môn học được chọn không tồn tại."));

        CloudStorage storage = cloudStorageRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu hình lưu trữ của tài khoản này."));

        long actualFileSize = requestDTO.getFileSize() != null ? requestDTO.getFileSize() : 0L;

        double usedQuota = storage.getUsedQuota() != null ? storage.getUsedQuota() : 0.0;
        double updatedUsedQuota = usedQuota + actualFileSize;

        double totalQuotaGb = getTotalQuotaGbForUser(user);
        double maxFileSizeMb = getMaxFileSizeMbForUser(user);

        if (actualFileSize > maxFileSizeMb * BYTES_PER_MB) {
            storageUploadService.logFailure(
                    storage,
                    requestDTO.getDocumentName(),
                    actualFileSize,
                    "FAILED_FILE_TOO_LARGE"
            );

            throw new IllegalArgumentException("Dung lượng file tối đa là " + maxFileSizeMb + "MB.");
        }

        if (updatedUsedQuota > totalQuotaGb * BYTES_PER_GB) {
            storageUploadService.logFailure(
                    storage,
                    requestDTO.getDocumentName(),
                    actualFileSize,
                    "FAILED_QUOTA_FULL"
            );

            throw new RuntimeException("Không gian lưu trữ của bạn đã đầy.");
        }

        Document document = new Document();
        document.setUser(user);
        document.setDocumentName(requestDTO.getDocumentName());
        SystemConfig systemConfig = systemConfigRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Cấu Hình Không Tồn Tại"));
        List<String> extensions = Arrays.asList(systemConfig.getAllowedFileTypes().split(","));
        if(!extensions.contains(requestDTO.getFileType().name())) {
            throw new RuntimeException("Định dạng file không được cho phép");
        }

        document.setFileType(requestDTO.getFileType());
        document.setFileSize(actualFileSize);
        document.setPreviewUrl(requestDTO.getPreviewUrl());
        document.setDownloadUrl(requestDTO.getDownloadUrl());
        document.setDescription(requestDTO.getDescription());
        document.setStatus(
                requestDTO.getStatus() != null
                        ? requestDTO.getStatus()
                        : StatusPublicDoc.DEFAULT
        );
        document.setPublic(false);
        document.setCategory(category);

        Document savedDocument = documentRepository.saveAndFlush(document);

        storage.setUsedQuota(updatedUsedQuota);
        cloudStorageRepository.saveAndFlush(storage);

        storageUploadService.logSuccess(storage, requestDTO.getDocumentName(), actualFileSize);

        try {
            String fileUrl = savedDocument.getDownloadUrl() != null && !savedDocument.getDownloadUrl().isBlank()
                    ? savedDocument.getDownloadUrl()
                    : savedDocument.getPreviewUrl();

            String fullTextContent = extractTextFromUrl(fileUrl, savedDocument.getFileType());

            if (fullTextContent != null && !fullTextContent.trim().isEmpty()) {
                System.out.println("==> RAG LOG: Trích xuất thành công " + fullTextContent.length() + " ký tự chữ từ file.");
                documentChunkService.chunkAndEmbedDocument(savedDocument, fullTextContent);
            } else {
                System.out.println("==> RAG WARNING: File rỗng hoặc không thể trích xuất chữ từ URL: " + fileUrl);
            }
        } catch (Exception e) {
            System.err.println("==> RAG ERROR: Lỗi trong quá trình đọc file và băm Chunk: " + e.getMessage());
        }

        return mapToResponseDTO(savedDocument);
    }

    @Override
    @Transactional
    public DocumentResponseDTO replaceDocumentFile(UUID documentId, DocumentRequestDTO requestDTO) {
        User user = getCurrentUser();
        UUID userId = user.getId();

        Document document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Tài liệu không tồn tại hoặc bạn không có quyền thay đổi file."));

        CloudStorage storage = cloudStorageRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu hình lưu trữ của tài khoản này."));

        long oldFileSize = document.getFileSize() != null ? document.getFileSize() : 0L;
        long newFileSize = requestDTO.getFileSize() != null ? requestDTO.getFileSize() : 0L;

        double usedQuota = storage.getUsedQuota() != null ? storage.getUsedQuota() : 0.0;
        double updatedUsedQuota = Math.max(0.0, usedQuota - oldFileSize) + newFileSize;

        double totalQuotaGb = getTotalQuotaGbForUser(user);
        double maxFileSizeMb = getMaxFileSizeMbForUser(user);

        if (newFileSize > maxFileSizeMb * BYTES_PER_MB) {
            storageUploadService.logFailure(
                    storage,
                    requestDTO.getDocumentName(),
                    newFileSize,
                    "FAILED_FILE_TOO_LARGE"
            );

            throw new IllegalArgumentException("Dung lượng file tối đa là " + maxFileSizeMb + "MB.");
        }

        if (updatedUsedQuota > totalQuotaGb * BYTES_PER_GB) {
            storageUploadService.logFailure(
                    storage,
                    requestDTO.getDocumentName(),
                    newFileSize,
                    "FAILED_QUOTA_FULL"
            );

            throw new RuntimeException("Không gian lưu trữ của bạn đã đầy.");
        }

        deletePhysicalFileFromSupabase(document);

        document.setDocumentName(requestDTO.getDocumentName());
        document.setFileType(requestDTO.getFileType());
        document.setFileSize(newFileSize);
        document.setPreviewUrl(requestDTO.getPreviewUrl());
        document.setDownloadUrl(requestDTO.getDownloadUrl());

        Document savedDocument = documentRepository.saveAndFlush(document);

        storage.setUsedQuota(updatedUsedQuota);
        cloudStorageRepository.saveAndFlush(storage);

        storageUploadService.logSuccess(storage, requestDTO.getDocumentName(), newFileSize);

        documentChunkRepository.deleteByDocument_Id(documentId);

        try {
            String fileUrl = savedDocument.getDownloadUrl() != null && !savedDocument.getDownloadUrl().isBlank()
                    ? savedDocument.getDownloadUrl()
                    : savedDocument.getPreviewUrl();

            String fullTextContent = extractTextFromUrl(fileUrl, savedDocument.getFileType());

            if (fullTextContent != null && !fullTextContent.trim().isEmpty()) {
                documentChunkService.chunkAndEmbedDocument(savedDocument, fullTextContent);
            }
        } catch (Exception e) {
            System.err.println("==> RAG ERROR: Lỗi trong quá trình đọc file mới và băm Chunk: " + e.getMessage());
        }

        return mapToResponseDTO(savedDocument);
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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu."));

        boolean isOwner = document.getUser() != null
                && Objects.equals(document.getUser().getId(), userId);

        boolean isPublic = document.isPublic();

        boolean isSharedWithMe =
                documentShareRepository.existsByDocument_IdAndSharedWithUser_Id(documentId, userId);

        boolean isStaff = isStaff(getAuthentication());

        if (!isOwner && !isPublic && !isSharedWithMe && !isStaff) {
            throw new RuntimeException("Bạn không có quyền xem tài liệu này.");
        }

        return mapToResponseDTO(document);
    }

    @Override
    @Transactional
    public DocumentResponseDTO updateDocument(UUID documentId, DocumentUpdateRequestDTO requestDTO) {
        User user = getCurrentUser();
        UUID userId = user.getId();

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu."));

        boolean isOwner = document.getUser() != null
                && Objects.equals(document.getUser().getId(), userId);

        java.util.Optional<DocumentShare> shareOpt =
                documentShareRepository.findByDocument_IdAndSharedWithUser_Id(documentId, userId);

        boolean hasEditPermission = shareOpt.isPresent()
                && "edit".equalsIgnoreCase(shareOpt.get().getPermissionType());

        if (!isOwner && !hasEditPermission) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa tài liệu này.");
        }

        if (requestDTO == null) {
            throw new RuntimeException("Dữ liệu cập nhật không hợp lệ.");
        }

        if (requestDTO.getDocumentName() != null && !requestDTO.getDocumentName().isBlank()) {
            document.setDocumentName(requestDTO.getDocumentName().trim());
        }

        if (requestDTO.getDescription() != null) {
            document.setDescription(requestDTO.getDescription().trim());
        }

        if (requestDTO.getCategoryId() != null) {
            DocumentCategory category = documentCategoryRepository.findById(requestDTO.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Môn học được chọn không tồn tại."));

            document.setCategory(category);
        }

        Document updatedDocument = documentRepository.saveAndFlush(document);

        return mapToResponseDTO(updatedDocument);
    }

    @Override
    @Transactional
    public DocumentResponseDTO updateDocumentName(UUID documentId, String newName) {
        DocumentUpdateRequestDTO requestDTO = new DocumentUpdateRequestDTO();
        requestDTO.setDocumentName(newName);

        return updateDocument(documentId, requestDTO);
    }

    @Override
    @Transactional
    public void deleteDocument(UUID documentId) {
        User user = getCurrentUser();
        UUID userId = user.getId();

        Document document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new RuntimeException("Tài liệu không tồn tại hoặc bạn không có quyền xóa."));

        long actualFileSize = document.getFileSize() != null ? document.getFileSize() : 0L;

        deletePhysicalFileFromSupabase(document);

        CloudStorage storage = cloudStorageRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Cấu hình lưu trữ không tồn tại."));

        double usedQuota = storage.getUsedQuota() != null ? storage.getUsedQuota() : 0.0;
        double newUsedQuota = Math.max(0.0, usedQuota - actualFileSize);

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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu."));

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

        boolean isStaff = isStaff(getAuthentication());

        if (!isOwner && !isPublic && !hasDownloadPermission && !isStaff) {
            throw new RuntimeException("Bạn không có quyền tải xuống tài liệu này.");
        }

        return fetchFileResourceFromCloud(document);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource getFileResourceForPreview(UUID documentId) {
        User user = getCurrentUser();
        UUID userId = user.getId();

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu."));

        boolean isOwner = document.getUser() != null
                && Objects.equals(document.getUser().getId(), userId);

        boolean isPublic = document.isPublic();

        boolean isSharedWithMe =
                documentShareRepository.existsByDocument_IdAndSharedWithUser_Id(documentId, userId);

        boolean isStaff = isStaff(getAuthentication());

        if (!isOwner && !isPublic && !isSharedWithMe && !isStaff) {
            throw new RuntimeException("Bạn không có quyền xem trước tài liệu này.");
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
            throw new RuntimeException("Bạn không có quyền chỉnh sửa trạng thái của tài liệu này.");
        }

        if (isPublic) {
            document.setPublic(false);
            document.setStatus(StatusPublicDoc.PENDING);
        } else {
            document.setPublic(false);
            document.setStatus(StatusPublicDoc.DENY);
            document.setApprovedBy(null);
        }

        Document updatedDocument = documentRepository.saveAndFlush(document);

        return mapToResponseDTO(updatedDocument);
    }

    @Override
    @Transactional
    public DocumentResponseDTO approvePublicRequest(UUID documentId, RequestPublicDoc decision) {
        if (decision == null) {
            throw new RuntimeException("Quyết định phê duyệt không hợp lệ.");
        }

        Authentication authentication = getAuthentication();

        if (!isStaff(authentication)) {
            throw new RuntimeException("Bạn không có quyền thực hiện thao tác duyệt này.");
        }

        User reviewer = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin người duyệt."));

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu yêu cầu phê duyệt."));

        if (document.getStatus() != StatusPublicDoc.PENDING) {
            throw new RuntimeException("Tài liệu này không có yêu cầu phê duyệt cần xử lý.");
        }

        if (decision == RequestPublicDoc.ACCEPT) {
            document.setPublic(true);
            document.setStatus(StatusPublicDoc.SUCCESS);
        } else if (decision == RequestPublicDoc.DENY) {
            document.setPublic(false);
            document.setStatus(StatusPublicDoc.DENY);
        } else {
            throw new RuntimeException("Quyết định phê duyệt không hợp lệ.");
        }

        document.setApprovedBy(reviewer);

        Document savedDocument = documentRepository.saveAndFlush(document);

        mailService.sendDocumentReviewResultEmail(
                savedDocument.getUser().getEmail(),
                savedDocument.getDocumentName(),
                decision.name()
        );

        mailService.sendDocumentReviewConfirmationEmail(
                reviewer.getEmail(),
                savedDocument.getDocumentName(),
                decision.name()
        );

        return mapToResponseDTO(savedDocument);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getTotalQuota() {
        User user = getCurrentUser();

        return getTotalQuotaGbForUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> getPublicDocuments() {
        return documentRepository.findByIsPublicTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    private double getTotalQuotaGbForUser(User user) {
        UserMemberSubscription memberSubscription = userMemberSubscriptionRepository.findByUser(user)
                .orElse(null);

        if (isActiveMember(memberSubscription)) {
            SubscriptionPlan subscriptionPlan = resolveSubscriptionPlan(memberSubscription);
            return subscriptionPlan.getTotalStorageQuotaGb() != null
                    ? subscriptionPlan.getTotalStorageQuotaGb()
                    : 0.0;
        }

        SystemConfig systemConfig = systemConfigRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Cấu hình hệ thống hiện chưa khả dụng."));

        return systemConfig.getTotalStorageQuotaGb() != null
                ? systemConfig.getTotalStorageQuotaGb()
                : 0.0;
    }

    private double getMaxFileSizeMbForUser(User user) {
        UserMemberSubscription memberSubscription = userMemberSubscriptionRepository.findByUser(user)
                .orElse(null);

        if (isActiveMember(memberSubscription)) {
            SubscriptionPlan subscriptionPlan = resolveSubscriptionPlan(memberSubscription);
            return subscriptionPlan.getMaxFileSizeMb() != null
                    ? subscriptionPlan.getMaxFileSizeMb()
                    : 0.0;
        }

        SystemConfig systemConfig = systemConfigRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Cấu hình hệ thống hiện chưa khả dụng."));

        return systemConfig.getMaxFileSizeMb() != null
                ? systemConfig.getMaxFileSizeMb()
                : 0.0;
    }

    private SubscriptionPlan resolveSubscriptionPlan(UserMemberSubscription memberSubscription) {
        if (memberSubscription != null && memberSubscription.getSubscriptionPlan() != null) {
            return memberSubscription.getSubscriptionPlan();
        }

        return subscriptionPlanRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("Gói Premium hiện chưa khả dụng."));
    }

    private boolean isActiveMember(UserMemberSubscription memberSubscription) {
        if (memberSubscription == null) {
            return false;
        }

        if (memberSubscription.getStatus() != MemberStatus.ACTIVE) {
            return false;
        }

        return memberSubscription.getEndDate() == null
                || memberSubscription.getEndDate().isAfter(Instant.now());
    }

    private Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
            throw new RuntimeException("Vui lòng đăng nhập để tiếp tục.");
        }

        return authentication;
    }

    private User getCurrentUser() {
        Authentication authentication = getAuthentication();

        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản hiện tại."));
    }

    private boolean isStaff(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority -> {
                    String value = authority.getAuthority();

                    return "ADMIN".equals(value)
                            || "ROLE_ADMIN".equals(value)
                            || "MODERATOR".equals(value)
                            || "ROLE_MODERATOR".equals(value);
                });
    }

    private String extractTextFromUrl(String fileUrl, FileType fileType) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return "";
        }

        String typeStr = fileType != null ? fileType.name().toLowerCase() : "";
        String lowerUrl = fileUrl.toLowerCase();

        try {
            java.net.URL url = java.net.URI.create(fileUrl).toURL();

            if (typeStr.contains("txt") || lowerUrl.endsWith(".txt")) {
                try (InputStream in = url.openStream()) {
                    return new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
            }

            if (typeStr.contains("pdf") || lowerUrl.endsWith(".pdf")) {
                try (InputStream in = url.openStream();
                     PDDocument pdfDocument = PDDocument.load(in)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    return stripper.getText(pdfDocument);
                }
            }

            if (typeStr.contains("docx") || lowerUrl.endsWith(".docx")) {
                try (InputStream in = url.openStream();
                     org.apache.poi.xwpf.usermodel.XWPFDocument docx =
                             new org.apache.poi.xwpf.usermodel.XWPFDocument(in);
                     org.apache.poi.xwpf.extractor.XWPFWordExtractor extractor =
                             new org.apache.poi.xwpf.extractor.XWPFWordExtractor(docx)) {
                    return extractor.getText();
                }
            }

            if (typeStr.contains("doc") || lowerUrl.endsWith(".doc")) {
                try (InputStream in = url.openStream();
                     org.apache.poi.hwpf.HWPFDocument doc =
                             new org.apache.poi.hwpf.HWPFDocument(in);
                     org.apache.poi.hwpf.extractor.WordExtractor extractor =
                             new org.apache.poi.hwpf.extractor.WordExtractor(doc)) {
                    return extractor.getText();
                }
            }

            if (typeStr.contains("xls") || lowerUrl.endsWith(".xlsx") || lowerUrl.endsWith(".xls")) {
                try (InputStream in = url.openStream();
                     org.apache.poi.ss.usermodel.Workbook workbook =
                             org.apache.poi.ss.usermodel.WorkbookFactory.create(in)) {
                    return extractTextFromExcel(workbook);
                }
            }
        } catch (Exception e) {
            System.err.println("==> LỖI BÓC TÁCH CHỮ TỪ URL SUPABASE: " + e.getMessage());
        }

        return "";
    }

    private String extractTextFromExcel(org.apache.poi.ss.usermodel.Workbook workbook) {
        StringBuilder sb = new StringBuilder();
        org.apache.poi.ss.usermodel.DataFormatter formatter = new org.apache.poi.ss.usermodel.DataFormatter();

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(i);
            sb.append("--- Trang (Sheet): ").append(sheet.getSheetName()).append(" ---\n");

            for (org.apache.poi.ss.usermodel.Row row : sheet) {
                StringBuilder rowBuilder = new StringBuilder();

                for (org.apache.poi.ss.usermodel.Cell cell : row) {
                    String cellValue = formatter.formatCellValue(cell).trim();

                    if (!cellValue.isEmpty()) {
                        rowBuilder.append(cellValue).append(" | ");
                    }
                }

                if (!rowBuilder.isEmpty()) {
                    sb.append(rowBuilder).append("\n");
                }
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    private void deletePhysicalFileFromSupabase(Document document) {
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

            if (serviceKey == null || serviceKey.isBlank()) {
                System.out.println("==> Cảnh báo: supabase.service-role-key trống, bỏ qua xóa file vật lý. FileKey=" + fileKey);
                return;
            }

            if (fileKey.startsWith("/")) {
                fileKey = fileKey.substring(1);
            }

            String baseUrl = supabaseUrl.endsWith("/")
                    ? supabaseUrl.substring(0, supabaseUrl.length() - 1)
                    : supabaseUrl;

            String deleteUrl = baseUrl + "/storage/v1/object/" + bucketName + "/" + fileKey;

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + serviceKey);
            headers.set("apikey", serviceKey);

            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);

            restTemplate.exchange(
                    deleteUrl,
                    org.springframework.http.HttpMethod.DELETE,
                    entity,
                    String.class
            );

            System.out.println("==> Đã gửi lệnh xóa file vật lý trên Supabase Bucket: " + fileKey);
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

        String target = "/" + bucketName + "/";
        int index = fileUrl.indexOf(target);

        if (index != -1) {
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

        if (document.getCategory() != null) {
            DocumentCategory category = document.getCategory();

            dto.setCategoryId(category.getId());
            dto.setCategoryName(category.getCategoryName());
            dto.setCategoryType(category.getCategoryType());
            dto.setParentCategoryId(category.getParentId());

            dto.setSubjectCode(category.getCategoryName());
            dto.setSubjectName(category.getCategoryName());
        }

        return dto;
    }
}
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
import swp391.aistudyhub.entity.DocumentShare;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.repository.*;
import swp391.aistudyhub.service.DocumentService;
import swp391.aistudyhub.service.StorageUploadService;

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

    @Autowired
    private jakarta.persistence.EntityManager entityManager;


    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private StorageUploadService storageUploadService;

    @Autowired
    private DocumentShareRepository documentShareRepository; // 🌟 Thêm dòng này để hết gạch đỏ

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

        Document savedDoc = documentRepository.saveAndFlush(doc);

        long realTotalUsedQuota = documentRepository.sumFileSizeByUserId(userId);

        storage.setUsedQuota(updatedUsedQuota);
        cloudStorageRepository.save(storage);

        storageUploadService.logSuccess(storage, requestDTO.getDocumentName(), actualFileSize);
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
        // 1. Tìm tài liệu trong hệ thống
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        // 2. Định nghĩa các điều kiện hợp lệ để được quyền xem file
        boolean isOwner = doc.getUser() != null && Objects.equals(doc.getUser().getId(), userId);
        boolean isPublic = doc.isPublic(); // Giả định trường check public trong Entity Document của bạn là isPublic hoặc getIsPublic()

        // Kiểm tra xem user này có được share đích danh trong bảng trung gian không
        boolean isSharedWithMe = documentShareRepository.existsByDocument_IdAndSharedWithUser_Id(documentId, userId);

        // 3. Nếu KHÔNG PHẢI chủ sở hữu, KHÔNG PHẢI file public, và CŨNG KHÔNG ĐƯỢC SHARE -> Chặn lại ngay
        if (!isOwner && !isPublic && !isSharedWithMe) {
            throw new RuntimeException("Bạn không có quyền xem tài liệu này");
        }

        return mapToResponseDTO(doc);
    }

    @Override
    @Transactional
    public DocumentResponseDTO updateDocumentName(UUID documentId, UUID userId, String newName) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        boolean isOwner = doc.getUser() != null && Objects.equals(doc.getUser().getId(), userId);

        // 🌟 THAY ĐỔI: Kiểm tra xem user hiện tại có được cấp quyền 'edit' thông qua bảng share không
        java.util.Optional<DocumentShare> shareOpt = documentShareRepository.findByDocument_IdAndSharedWithUser_Id(documentId, userId);
        boolean hasEditPermission = shareOpt.isPresent() && "edit".equalsIgnoreCase(shareOpt.get().getPermissionType());

        // Chỉ cho phép chỉnh sửa nếu là Chủ sở hữu HOẶC có quyền 'edit'
        if (!isOwner && !hasEditPermission) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa tài liệu này!");
        }

        doc.setDocumentName(newName);
        Document updatedDoc = documentRepository.save(doc);

        return mapToResponseDTO(updatedDoc);
    }



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

        boolean isOwner = doc.getUser() != null && java.util.Objects.equals(doc.getUser().getId(), userId);
        boolean isPublic = doc.isPublic();

        // 🌟 THAY ĐỔI: Tìm bản ghi share cụ thể để bóc tách loại quyền (permissionType)
        java.util.Optional<DocumentShare> shareOpt = documentShareRepository.findByDocument_IdAndSharedWithUser_Id(documentId, userId);

        boolean hasDownloadPermission = false;
        if (shareOpt.isPresent()) {
            String permission = shareOpt.get().getPermissionType();
            // Hợp lệ nếu quyền là 'download' hoặc quyền 'edit' (quyền 'view' sẽ trả về false)
            if ("download".equalsIgnoreCase(permission) || "edit".equalsIgnoreCase(permission)) {
                hasDownloadPermission = true;
            }
        }

        // Chặn nếu không thỏa mãn bất kỳ điều kiện nào (Không phải chủ, không public, không có quyền tải)
        if (!isOwner && !isPublic && !hasDownloadPermission) {
            throw new RuntimeException("Tài liệu này chỉ cho phép xem trực tuyến, bạn không có quyền tải xuống!");
        }

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

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> searchAndFilterDocuments(UUID userId, String searchName, String fileType) {
        List<Document> accessibleDocs = documentRepository.findAccessibleDocuments(userId);

        return accessibleDocs.stream()
                .filter(doc -> {
                    if (searchName != null && !searchName.trim().isEmpty()) {
                        return doc.getDocumentName() != null &&
                                doc.getDocumentName().toLowerCase().contains(searchName.toLowerCase().trim());
                    }
                    return true;
                })
                .filter(doc -> {
                    if (fileType != null && !fileType.trim().isEmpty()) {
                        return doc.getFileType() != null &&
                                doc.getFileType().toLowerCase().contains(fileType.toLowerCase().trim());
                    }
                    return true;
                })
                .map(this::mapToResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public DocumentResponseDTO toggleDocumentPublicStatus(UUID userId, UUID documentId, boolean isPublic) {
        // 1. Tìm tài liệu dựa trên ID thô gửi lên
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu yêu cầu."));

        // 2. Bảo mật: Kiểm tra xem User đang thực hiện có đúng là chủ sở hữu của tài liệu này không
        if (!document.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa trạng thái của tài liệu này!");
        }

        // 3. Tiến hành thay đổi trạng thái lưu trữ
        document.setPublic(isPublic);
        Document updatedDoc = documentRepository.saveAndFlush(document);

        // 4. Đóng gói dữ liệu trả về thông qua hàm map có sẵn trong service của bạn
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
}
package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.aistudyhub.entity.Document;
import swp391.aistudyhub.entity.DocumentShare;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.repository.DocumentRepository;
import swp391.aistudyhub.repository.DocumentShareRepository;
import swp391.aistudyhub.repository.UserRepository;
import swp391.aistudyhub.service.DocumentShareService;

import java.util.Objects;
import java.util.UUID;

@Service
public class DocumentShareServiceImpl implements DocumentShareService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentShareRepository documentShareRepository;

    @Override
    @Transactional
    public void shareDocumentToUser(UUID documentId, UUID targetUserId, String permissionType) {
        User currentUser = getCurrentUser();
        UUID ownerId = currentUser.getId();

        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu yêu cầu."));

        if (doc.getUser() == null || !Objects.equals(doc.getUser().getId(), ownerId)) {
            throw new RuntimeException("Bạn không có quyền chia sẻ tài liệu này!");
        }

        if (Objects.equals(ownerId, targetUserId)) {
            throw new RuntimeException("Bạn không thể tự chia sẻ tài liệu cho chính bản thân.");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng được chia sẻ trên hệ thống."));

        boolean alreadyShared =
                documentShareRepository.existsByDocument_IdAndSharedWithUser_Id(documentId, targetUserId);

        if (alreadyShared) {
            throw new RuntimeException("Tài liệu này đã được chia sẻ cho người dùng này từ trước.");
        }

        DocumentShare share = new DocumentShare();
        share.setDocument(doc);
        share.setSharedWithUser(targetUser);
        share.setPermissionType(normalizePermission(permissionType));

        documentShareRepository.save(share);
    }

    @Override
    @Transactional
    public void updateSharePermission(UUID documentId, UUID targetUserId, String newPermissionType) {
        User currentUser = getCurrentUser();
        UUID ownerId = currentUser.getId();

        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu."));

        if (doc.getUser() == null || !Objects.equals(doc.getUser().getId(), ownerId)) {
            throw new RuntimeException("Bạn không phải chủ sở hữu để thay đổi quyền tài liệu này!");
        }

        DocumentShare share = documentShareRepository.findByDocument_IdAndSharedWithUser_Id(documentId, targetUserId)
                .orElseThrow(() -> new RuntimeException("Tài liệu này chưa từng được chia sẻ cho người dùng này."));

        share.setPermissionType(normalizePermission(newPermissionType));

        documentShareRepository.save(share);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
            throw new RuntimeException("You are not login yet!");
        }

        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new RuntimeException("This user is not found!"));
    }

    private String normalizePermission(String permissionType) {
        String permission =
                permissionType != null && !permissionType.trim().isEmpty()
                        ? permissionType.trim().toLowerCase()
                        : "view";

        if (!permission.equals("view")
                && !permission.equals("download")
                && !permission.equals("edit")) {
            throw new RuntimeException("Loại quyền không hợp lệ! Chỉ chấp nhận: view, download, edit.");
        }

        return permission;
    }
}
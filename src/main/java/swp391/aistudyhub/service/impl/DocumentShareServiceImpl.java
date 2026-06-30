package swp391.aistudyhub.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
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
    public void shareDocumentToUser(UUID ownerId, UUID documentId, UUID targetUserId, String permissionType) {
        // 1. Kiểm tra tài liệu tồn tại
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu yêu cầu."));

        // 2. Bảo mật: Chỉ chủ sở hữu đích thực của tài liệu mới được quyền đem đi chia sẻ
        if (doc.getUser() == null || !Objects.equals(doc.getUser().getId(), ownerId)) {
            throw new RuntimeException("Bạn không có quyền chia sẻ tài liệu này!");
        }

        // 3. Không cho phép tự chia sẻ cho chính mình
        if (Objects.equals(ownerId, targetUserId)) {
            throw new RuntimeException("Bạn không thể tự chia sẻ tài liệu cho chính bản thân.");
        }

        // 4. Kiểm tra User được nhận share có tồn tại trong hệ thống không
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng được chia sẻ trên hệ thống."));

        // 5. Kiểm tra xem tài liệu này đã từng được share cho User này chưa (tránh trùng lặp gây lỗi UniqueConstraint)
        boolean alreadyShared = documentShareRepository.existsByDocument_IdAndSharedWithUser_Id(documentId, targetUserId);
        if (alreadyShared) {
            throw new RuntimeException("Tài liệu này đã được chia sẻ cho người dùng này từ trước.");
        }

        // 6. Tiến hành lưu bản ghi share mới kèm kiểm tra Whitelist quyền
        DocumentShare share = new DocumentShare();
        share.setDocument(doc);
        share.setSharedWithUser(targetUser);

        // 🌟 THAY ĐỔI TẠI ĐÂY: Chuẩn hóa và ép chặt 3 bộ từ khóa quyền hợp lệ
        if (permissionType != null && !permissionType.trim().isEmpty()) {
            String pType = permissionType.trim().toLowerCase();

            if (!pType.equals("view") && !pType.equals("download") && !pType.equals("edit")) {
                throw new RuntimeException("Loại quyền không hợp lệ! Chỉ chấp nhận: view, download, edit");
            }
            share.setPermissionType(pType);
        } else {
            // Mặc định nếu Frontend không truyền param này lên hệ thống thì sẽ gán quyền cơ bản nhất là chỉ xem
            share.setPermissionType("view");
        }

        documentShareRepository.save(share);
    }
}
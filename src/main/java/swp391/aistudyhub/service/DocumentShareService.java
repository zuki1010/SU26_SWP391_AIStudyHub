package swp391.aistudyhub.service;

import java.util.UUID; // 🌟 Đảm bảo import đúng dòng này

public interface DocumentShareService {
    // Ép hẳn package java.util.UUID vào tham số để tránh import nhầm
    void shareDocumentToUser(java.util.UUID ownerId, java.util.UUID documentId, java.util.UUID targetUserId, String permissionType);
}
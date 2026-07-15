package swp391.aistudyhub.service;

import java.util.UUID;

public interface DocumentShareService {

    void shareDocumentToUser(UUID documentId, UUID targetUserId, String permissionType);

    void updateSharePermission(UUID documentId, UUID targetUserId, String newPermissionType);
}
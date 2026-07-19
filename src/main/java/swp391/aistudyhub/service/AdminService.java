package swp391.aistudyhub.service;

import org.springframework.data.domain.Page;
import swp391.aistudyhub.dto.response.AdminChatResponseDTO;
import swp391.aistudyhub.dto.response.AdminDocumentResponseDTO;
import swp391.aistudyhub.dto.response.AdminStorageResponseDTO;
import swp391.aistudyhub.dto.response.UserAccountResponseDTO;
import swp391.aistudyhub.enums.AccountStatus;
import swp391.aistudyhub.enums.UserRole;

import java.util.UUID;

public interface AdminService {

    Page<UserAccountResponseDTO> getAllCustomer(String key, int page, int size);

    UserAccountResponseDTO updateUserStatus(UUID id, AccountStatus status);

    UserAccountResponseDTO updateUserRole(UUID id, UserRole role);

    Page<AdminDocumentResponseDTO> getAllDocument(
            String key,
            String status,
            Boolean isPublic,
            int page,
            int size
    );

    Page<AdminChatResponseDTO> getAllChat(int page, int size);

    Page<AdminStorageResponseDTO> getAllStorage(int page, int size);
}
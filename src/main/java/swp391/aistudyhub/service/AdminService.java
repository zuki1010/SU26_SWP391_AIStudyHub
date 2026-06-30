package swp391.aistudyhub.service;

import org.springframework.data.domain.Page;
import swp391.aistudyhub.dto.projection.DocumentResponse;
import swp391.aistudyhub.dto.projection.UserAccountResponse;
import swp391.aistudyhub.dto.response.UserAccountResponseDTO;
import swp391.aistudyhub.entity.Document;
import swp391.aistudyhub.entity.User;
import swp391.aistudyhub.enums.AccountStatus;
import swp391.aistudyhub.enums.UserRole;

import java.util.List;
import java.util.UUID;

public interface AdminService {
    Page<UserAccountResponse> getAllCustomer(String key, int page, int size);

    UserAccountResponse updateUserStatus(UUID id, AccountStatus status);

    Page<DocumentResponse> getAllDocument(int page, int size);

    UserAccountResponse updateUserRole(UUID id, UserRole role);
}

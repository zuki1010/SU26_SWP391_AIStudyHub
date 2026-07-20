package swp391.aistudyhub.service;

import org.springframework.data.domain.Page;
import swp391.aistudyhub.dto.projection.ChatRequestResponse;
import swp391.aistudyhub.dto.projection.DocumentResponse;
import swp391.aistudyhub.dto.projection.StorageUsageResponse;
import swp391.aistudyhub.dto.projection.UserAccountResponse;
import swp391.aistudyhub.dto.request.MemberConfigDTO;
import swp391.aistudyhub.dto.request.SystemConfigDTO;
import swp391.aistudyhub.enums.AccountStatus;
import swp391.aistudyhub.enums.StatusPublicDoc;
import swp391.aistudyhub.enums.UserRole;

import java.math.BigDecimal;
import java.util.UUID;

public interface AdminService {

    Page<UserAccountResponse> getAllCustomer(String key, int page, int size);

    UserAccountResponse updateUserStatus(UUID id, AccountStatus status);

    Page<DocumentResponse> getAllDocument(int page, int size);

    UserAccountResponse updateUserRole(UUID id, UserRole role);

    Page<ChatRequestResponse> getAllChat(int page, int size);

    Page<StorageUsageResponse> getAllStorage(int page, int size);

    void systemConfig(SystemConfigDTO dto);

    void memberConfig(MemberConfigDTO dto);

    void updatePriceMember(BigDecimal price);
}
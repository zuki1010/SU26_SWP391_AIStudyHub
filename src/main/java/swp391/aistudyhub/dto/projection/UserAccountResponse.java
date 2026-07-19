package swp391.aistudyhub.dto.projection;

import swp391.aistudyhub.enums.AccountStatus;
import swp391.aistudyhub.enums.UserRole;

import java.time.Instant;
import java.util.UUID;

public interface UserAccountResponse {
    String getEmail();

    AccountStatus getAccountStatus();

    Instant getCreatedAt();

    String getCustomerProfileFullName();

    String getModeratorProfileFullName();

    String getAdminProfileFullName();

    UserRole getRole();
}

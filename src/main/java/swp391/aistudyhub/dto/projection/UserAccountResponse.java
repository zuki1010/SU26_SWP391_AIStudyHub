package swp391.aistudyhub.dto.projection;

import swp391.aistudyhub.enums.AccountStatus;

import java.time.Instant;

public interface UserAccountResponse {
    String getEmail();
    AccountStatus getAccountStatus();
    Instant getCreatedAt();

    String getCustomerProfileFullName();

    String getModeratorProfileFullName();

    String getAdminProfileFullName();
}

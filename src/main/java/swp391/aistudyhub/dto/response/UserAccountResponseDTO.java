package swp391.aistudyhub.dto.response;

import swp391.aistudyhub.enums.AccountStatus;

import java.time.Instant;

public class UserAccountResponseDTO {
    private String email;
    private String fullName;
    private AccountStatus status;
    private Instant createdAt;
}

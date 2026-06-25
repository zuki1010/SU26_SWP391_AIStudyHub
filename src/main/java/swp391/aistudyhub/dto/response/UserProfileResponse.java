package swp391.aistudyhub.dto.response;

import lombok.Builder;
import lombok.Getter;
import swp391.aistudyhub.enums.AccountStatus;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class UserProfileResponse {

    private final UUID id;
    private final String email;
    private final String role;
    private final AccountStatus accountStatus;
    private final Instant createdAt;
    private final String fullName;
    private final String studentCode;
    private final String schoolName;
    private final String department;
    private final String assignedSubject;
    private final Integer accessLevel;
}

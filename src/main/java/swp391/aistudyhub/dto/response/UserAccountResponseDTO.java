package swp391.aistudyhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import swp391.aistudyhub.enums.AccountStatus;
import swp391.aistudyhub.enums.UserRole;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserAccountResponseDTO {

    private UUID id;

    private String email;

    private String fullName;

    /**
     * Field chuẩn cho FE mới.
     */
    private AccountStatus accountStatus;

    /**
     * Field alias để tương thích FE cũ đang dùng "status".
     */
    private AccountStatus status;

    private Instant createdAt;

    private UserRole role;
}
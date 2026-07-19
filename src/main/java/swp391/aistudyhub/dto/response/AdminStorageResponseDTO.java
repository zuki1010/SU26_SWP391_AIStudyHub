package swp391.aistudyhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminStorageResponseDTO {

    private UUID storageId;

    private UUID userId;

    private String userEmail;

    private String userFullName;

    private String userRole;

    private Long usedQuota;

    private Long totalQuota;

    private String usagePercentage;
}
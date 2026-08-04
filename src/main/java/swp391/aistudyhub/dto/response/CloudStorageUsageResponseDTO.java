package swp391.aistudyhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CloudStorageUsageResponseDTO {

    private UUID userId;

    private Double usedQuota;

    private Double totalQuota;

    private String usagePercentage;
}
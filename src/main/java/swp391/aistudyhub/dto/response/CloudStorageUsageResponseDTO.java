package swp391.aistudyhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CloudStorageUsageResponseDTO {
    private UUID userId;
    private long usedQuota;       // Dung lượng đã dùng (bytes)
    private long totalQuota;      // Tổng dung lượng cho phép (5GB - bytes)
    private String percentageUsed; // Tỷ lệ phần trăm đã dùng (ví dụ: 12.34%)
}

package swp391.aistudyhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import swp391.aistudyhub.enums.ReportReason;

@Data
@AllArgsConstructor
public class ReportReasonResponseDTO {
    private ReportReason code;       // Ví dụ: SPAM_OR_MISLEADING (Gửi về Backend)
    private String description;
}

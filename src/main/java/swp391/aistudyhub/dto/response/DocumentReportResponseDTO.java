package swp391.aistudyhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import swp391.aistudyhub.enums.ReportReason;
import swp391.aistudyhub.enums.ReportStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentReportResponseDTO {
    private UUID id;
    private UUID documentId;
    private String documentName;
    private UUID reporterId;
    private String reporterName;
    private ReportReason reason;
    private String reasonDescription;
    private String description;
    private ReportStatus status;
    private String adminNote;
    private LocalDateTime createdAt;
}
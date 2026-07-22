package swp391.aistudyhub.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import swp391.aistudyhub.enums.ReportStatus;

@Data
public class ProcessReportDTO {
    @NotNull(message = "Trạng thái không được để trống (RESOLVED hoặc REJECTED)")
    private ReportStatus status;

    private String adminNote;
}

package swp391.aistudyhub.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import swp391.aistudyhub.enums.ReportReason;

import java.util.UUID;

@Data
public class CreateReportDTO {

    @NotNull(message = "Document ID không được để trống")
    private UUID documentId;

    @NotNull(message = "Vui lòng chọn vấn đề báo cáo")
    private ReportReason reason;

    // Không dùng @NotBlank nữa để lý do thường có thể để trống hoặc điền tùy ý
    private String description;

    // 🔴 Validation custom: Bắt buộc nhập description nếu lý do là OTHER
    @AssertTrue(message = "Khi chọn 'Lý do khác', bạn bắt buộc phải nhập mô tả chi tiết vấn đề!")
    public boolean isValidDescription() {
        if (reason == ReportReason.OTHER) {
            return description != null && !description.trim().isEmpty();
        }
        return true; // Các lý do khác thì không bắt buộc
    }
}
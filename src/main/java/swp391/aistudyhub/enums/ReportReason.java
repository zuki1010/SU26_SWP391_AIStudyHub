package swp391.aistudyhub.enums;

import lombok.Getter;

@Getter
public enum ReportReason {
    SPAM_OR_MISLEADING("Spam hoặc thông tin sai lệch"),
    INAPPROPRIATE_CONTENT("Nội dung không phù hợp, đồi trụy, bạo lực"),
    COPYRIGHT_VIOLATION("Vi phạm bản quyền / Bản quyền thuộc về tác giả khác"),
    WRONG_SUBJECT_OR_CATEGORY("Sai môn học hoặc danh mục"),
    CORRUPTED_OR_UNREADABLE("File hỏng, lỗi font, không đọc được"),
    OTHER("Lý do khác");

    private final String description;

    ReportReason(String description) {
        this.description = description;
    }
}

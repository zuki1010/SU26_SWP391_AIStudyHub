package swp391.aistudyhub.entity;

import jakarta.persistence.*;
import lombok.*;
import swp391.aistudyhub.enums.ReportReason;
import swp391.aistudyhub.enums.ReportStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Tài liệu bị báo cáo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    // Người thực hiện báo cáo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    // Lý do báo cáo (Chọn từ Enum)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reason;

    // Mô tả chi tiết vấn đề từ User
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    // Trạng thái xử lý
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    // Ghi chú của Mod/Admin khi xử lý (ví dụ: "Đã gỡ bài do vi phạm bản quyền")
    @Column(columnDefinition = "TEXT")
    private String adminNote;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ReportStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
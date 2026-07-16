package swp391.aistudyhub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "system_config")
public class SystemConfig {

    @Id
    private Long id = 1L; // Cố định ID là 1, không dùng GeneratedValue tự tăng

    @Column(name = "max_daily_chat_tokens", nullable = false)
    private Integer maxDailyChatTokens;

    @Column(name = "total_storage_quota_gb", nullable = false)
    private Integer totalStorageQuotaGb;

    @Column(name = "max_file_size_mb", nullable = false)
    private Integer maxFileSizeMb;

    @Column(name = "allowed_file_types", nullable = false)
    private String allowedFileTypes;
}

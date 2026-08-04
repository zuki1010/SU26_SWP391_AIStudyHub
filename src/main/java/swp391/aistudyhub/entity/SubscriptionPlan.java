package swp391.aistudyhub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "subscription_plan")
public class SubscriptionPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private Integer planId;

    @Column(name = "plan_name", length = 50, nullable = false)
    private String planName;

    @Column(name = "price", precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "max_daily_chat_tokens", nullable = false)
    private Integer maxDailyChatTokens;

    @Column(name = "total_storage_quota_gb", nullable = false)
    private Double totalStorageQuotaGb;

    @Column(name = "max_file_size_mb", nullable = false)
    private Double maxFileSizeMb;
}

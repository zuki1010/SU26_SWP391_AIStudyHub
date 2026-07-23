package swp391.aistudyhub.dto.projection;

public interface SubscriptionPlanResponse {
    Integer getMaxDailyChatTokens();

    Long getTotalStorageQuotaGb();

    Long getMaxFileSizeMb();
}

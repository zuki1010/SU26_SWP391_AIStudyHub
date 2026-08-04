package swp391.aistudyhub.dto.projection;

public interface SubscriptionPlanResponse {
    Integer getMaxDailyChatTokens();

    Double getTotalStorageQuotaGb();

    Double getMaxFileSizeMb();
}

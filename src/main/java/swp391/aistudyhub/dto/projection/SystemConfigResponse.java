package swp391.aistudyhub.dto.projection;

public interface SystemConfigResponse {
    Integer getMaxDailyChatTokens();

    Long getTotalStorageQuotaGb();

    Long getMaxFileSizeMb();

    String getAllowedFileTypes();

}

package swp391.aistudyhub.dto.projection;

public interface SystemConfigResponse {
    Integer getMaxDailyChatTokens();

    Double getTotalStorageQuotaGb();

    Double getMaxFileSizeMb();

    String getAllowedFileTypes();

}

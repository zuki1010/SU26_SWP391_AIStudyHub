package swp391.aistudyhub.dto.request;

import lombok.Data;

@Data
public class SystemConfigDTO {

    private Integer maxDailyChatTokens;

    private Double totalStorageQuotaGb;

    private Double maxFileSizeMb;

    private String allowedFileTypes;
}
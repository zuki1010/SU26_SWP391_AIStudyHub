package swp391.aistudyhub.dto.request;

import lombok.Data;

@Data
public class SystemConfigDTO {
    private Integer maxDailyChatTokens;
    private Long totalStorageQuotaGb;
    private Long maxFileSizeMb;
    private String allowedFileTypes;

}

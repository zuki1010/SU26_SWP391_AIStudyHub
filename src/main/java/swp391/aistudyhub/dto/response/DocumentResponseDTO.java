package swp391.aistudyhub.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.UUID;
import java.time.Instant;

@Data
public class    DocumentResponseDTO {
    private UUID documentId;
    private String documentName;
    private String fileType;
    private String previewUrl;
    private String downloadUrl;
    private Instant createdAt;
    private String description;

    @JsonProperty("storageWarningMessage")
    private String storageWarningMessage;
}
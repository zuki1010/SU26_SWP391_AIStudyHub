package swp391.aistudyhub.dto.response;

import lombok.Data;
import java.util.UUID;
import java.time.Instant;

@Data
public class DocumentResponseDTO {
    private UUID documentId;
    private String documentName;
    private String fileType;
    private String previewUrl;
    private String downloadUrl;
    private Instant createdAt;

    private Long fileSize;
    private String description;
    private String textContent;
    private Boolean isPublic;
}
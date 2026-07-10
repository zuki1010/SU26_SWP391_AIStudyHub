package swp391.aistudyhub.dto.response;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class ForumPostResponseDTO {
    private UUID id;
    private UUID documentId;
    private UUID userId;
    private String userName;
    private String visibility;
    private String title;
    private String content;
    private String status;
    private Boolean isPinned;
    private Instant createdAt;
    private Instant updatedAt;
}

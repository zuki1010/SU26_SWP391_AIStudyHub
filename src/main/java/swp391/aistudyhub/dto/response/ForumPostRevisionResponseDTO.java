package swp391.aistudyhub.dto.response;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class ForumPostRevisionResponseDTO {
    private UUID id;
    private UUID postId;
    private UUID revisionNo;
    private String title;
    private String content;
    private String status;
    private UUID editedBy;
    private String editedByName;
    private Instant createdAt;
}

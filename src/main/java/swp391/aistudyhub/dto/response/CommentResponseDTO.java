package swp391.aistudyhub.dto.response;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class CommentResponseDTO {
    private UUID commentId;
    private UUID documentId;
    private UUID userId;
    private String content;
    private Instant createdAt;
}

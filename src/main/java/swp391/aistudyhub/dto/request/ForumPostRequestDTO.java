package swp391.aistudyhub.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class ForumPostRequestDTO {
    private UUID documentId;   // Tài liệu đính kèm bài viết (có thể null)
    private String title;
    private String content;
    private String visibility; // PUBLIC / PRIVATE
}

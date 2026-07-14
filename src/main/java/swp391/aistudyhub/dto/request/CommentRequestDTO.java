package swp391.aistudyhub.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class CommentRequestDTO {
    private UUID documentId; // Tài liệu được bình luận
    private String content;  // Nội dung bình luận
}

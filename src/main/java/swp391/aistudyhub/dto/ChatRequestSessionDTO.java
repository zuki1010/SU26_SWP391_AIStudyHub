package swp391.aistudyhub.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ChatRequestSessionDTO {
    private UUID sessionId;
    private String messageContent;
    private UUID documentId;
}

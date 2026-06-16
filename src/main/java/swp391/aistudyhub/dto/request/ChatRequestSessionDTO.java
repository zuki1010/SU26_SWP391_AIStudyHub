package swp391.aistudyhub.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class ChatRequestSessionDTO {
    private UUID sessionId;
    private String messageContent;
}

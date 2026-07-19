package swp391.aistudyhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminChatResponseDTO {

    private UUID messageId;

    private UUID sessionId;

    private String sessionTitle;

    private String messageContent;

    private String senderType;

    private Instant sentAt;

    private UUID userId;

    private String userEmail;

    private String userFullName;
}
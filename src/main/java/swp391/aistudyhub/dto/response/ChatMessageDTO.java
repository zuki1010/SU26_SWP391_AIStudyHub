package swp391.aistudyhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDTO {
    UUID id;
    String messageContent;
    String senderType;
    Instant sentAt;
}

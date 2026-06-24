package swp391.aistudyhub.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class NotificationResponse {
    private final UUID notificationId;
    private final String type;
    private final String message;
    private final UUID actorId;
    private final String actorName;
    private final UUID targetId;
    private final boolean read;
    private final Instant createdAt;
}

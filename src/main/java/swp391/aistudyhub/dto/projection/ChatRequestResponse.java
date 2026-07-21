package swp391.aistudyhub.dto.projection;

import org.springframework.beans.factory.annotation.Value;

import java.util.UUID;

public interface ChatRequestResponse {
    UUID getId();

    String getMessageContent();

    UUID getUserId();
}

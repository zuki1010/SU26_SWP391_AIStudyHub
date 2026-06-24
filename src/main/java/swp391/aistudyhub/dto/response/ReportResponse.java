package swp391.aistudyhub.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ReportResponse {
    private final UUID reportId;
    private final String targetType;
    private final UUID targetId;
    private final String reason;
    private final String status;
    private final UUID reporterId;
    private final String reporterEmail;
    private final UUID handledById;
    private final Instant handledAt;
    private final Instant createdAt;
}

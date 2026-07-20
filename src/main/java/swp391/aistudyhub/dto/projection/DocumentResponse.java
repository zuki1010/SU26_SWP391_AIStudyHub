package swp391.aistudyhub.dto.projection;

import swp391.aistudyhub.enums.StatusPublicDoc;

import java.time.Instant;
import java.util.UUID;

public interface DocumentResponse {

    UUID getDocumentId();

    String getDocumentName();

    Long getFileSize();

    Instant getCreatedAt();

    String getUserEmail();

    String getUserFullName();

    UUID getUserId();

    Boolean getIsPublic();

    String getStatus();

    String getUserName();
}
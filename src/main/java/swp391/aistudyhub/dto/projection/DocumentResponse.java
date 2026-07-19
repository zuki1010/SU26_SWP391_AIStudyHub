package swp391.aistudyhub.dto.projection;

import org.springframework.beans.factory.annotation.Value;
import swp391.aistudyhub.enums.UserRole;
import swp391.aistudyhub.repository.AdminProfileRepository;
import swp391.aistudyhub.repository.CustomerProfileRepository;
import swp391.aistudyhub.repository.ModeratorProfileRepository;
import swp391.aistudyhub.repository.UserRepository;

import java.time.Instant;
import java.util.UUID;

public interface DocumentResponse {
    String getDocumentName();

    Long getFileSize();

    Instant getCreatedAt();

    Boolean getIsPublic();

    // Tự động map từ "AS userEmail"
    String getUserEmail();

    // Tự động map từ "AS userFullName"
    String getUserFullName();
}

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

    @Value("#{target.user?.customerProfile != null ? target.user.customerProfile.fullName : target.user?.moderatorProfile?.fullName}")
    String getUserFullName();

    @Value("#{target.user?.email}")
    String getUserEmail();

    boolean getDocumentIsPublic();
}

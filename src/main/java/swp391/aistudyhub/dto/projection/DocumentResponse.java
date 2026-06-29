package swp391.aistudyhub.dto.projection;

import swp391.aistudyhub.repository.UserRepository;

import java.time.Instant;
import java.util.UUID;

public interface DocumentResponse {
    String getDocumentName();
    Long getFileSize();
    Instant getCreatedAt();
    UUID getUserId();

    default String getUserEmail(UserRepository userrepo) {
        if(getUserId() == null) return "N/A";
        return userrepo.findUserById(getUserId())
                .map(user -> user.getEmail())
                .orElse("N/A");
    }

}

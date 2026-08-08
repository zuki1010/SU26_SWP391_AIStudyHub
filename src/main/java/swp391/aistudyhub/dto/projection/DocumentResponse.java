package swp391.aistudyhub.dto.projection;

import swp391.aistudyhub.enums.FileType;
import swp391.aistudyhub.enums.StatusPublicDoc;

import java.time.Instant;
import java.util.UUID;

public interface DocumentResponse {

    UUID getDocumentId();

    String getDocumentName();

    FileType getFileType();

    Long getFileSize();

    Instant getCreatedAt();

    Boolean getIsPublic();

    StatusPublicDoc getStatus();

    UUID getCategoryId();

    String getCategoryName();

    String getCategoryType();

    UUID getParentCategoryId();

    UUID getUserId();

    String getUserEmail();

    String getUserFullName();

    String getAuthorName();

    String getUploaderName();

    String getUserName();
}
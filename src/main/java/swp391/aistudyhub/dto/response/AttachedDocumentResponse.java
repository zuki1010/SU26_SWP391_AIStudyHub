package swp391.aistudyhub.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AttachedDocumentResponse {
    private final UUID documentId;
    private final String documentName;
    private final String fileType;
    private final String previewUrl;
    private final String downloadUrl;
}

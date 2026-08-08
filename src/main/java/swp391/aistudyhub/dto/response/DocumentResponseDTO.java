package swp391.aistudyhub.dto.response;

import lombok.Data;
import swp391.aistudyhub.enums.FileType;
import swp391.aistudyhub.enums.StatusPublicDoc;

import java.time.Instant;
import java.util.UUID;

@Data
public class DocumentResponseDTO {

    private UUID documentId;

    private String documentName;

    private FileType fileType;

    private Long fileSize;

    private String previewUrl;

    private String downloadUrl;

    private Instant createdAt;

    private String description;

    private String textContent;

    private Boolean isPublic;

    private StatusPublicDoc status;

    private UUID categoryId;

    private String categoryName;

    private String categoryType;

    private UUID parentCategoryId;

    private String subjectCode;

    private String subjectName;
}
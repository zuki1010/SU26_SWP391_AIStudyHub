package swp391.aistudyhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminDocumentResponseDTO {

    private UUID documentId;

    private String documentName;

    private String fileType;

    private Long fileSize;

    private String previewUrl;

    private String downloadUrl;

    private Boolean isPublic;

    private String status;

    private Instant createdAt;

    private String description;

    private UUID userId;

    private String userEmail;

    private String userFullName;

    private String userRole;

    private UUID approvedById;

    private String approvedByEmail;

    private String approvedByName;
}
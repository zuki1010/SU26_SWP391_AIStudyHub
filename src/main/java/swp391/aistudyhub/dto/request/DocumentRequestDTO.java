package swp391.aistudyhub.dto.request;

import lombok.Data;
import swp391.aistudyhub.entity.DocumentCategory;
import swp391.aistudyhub.enums.FileType;
import swp391.aistudyhub.enums.StatusPublicDoc;
import swp391.aistudyhub.enums.SubjectCode;

import java.util.UUID;

@Data
public class DocumentRequestDTO {

    private String documentName;

    private FileType fileType;

    private String previewUrl;

    private String downloadUrl;

    private Long fileSize;

    private String textContent;

    private String description;

    private UUID categoryId;

    private StatusPublicDoc status;
}
package swp391.aistudyhub.dto.request;

import lombok.Data;
import swp391.aistudyhub.enums.FileType;

import java.util.List;

@Data
public class DocumentRequestDTO {

    private String documentName;

    private FileType fileType;

    private String previewUrl;

    private String downloadUrl;

    private Long fileSize;

    private String textContent;

    private String description;

    private List<String> categoryNames;
}
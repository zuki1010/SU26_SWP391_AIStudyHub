package swp391.aistudyhub.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class DocumentUpdateRequestDTO {

    private String documentName;

    private String description;

    private UUID categoryId;
}
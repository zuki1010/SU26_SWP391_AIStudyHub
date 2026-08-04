package swp391.aistudyhub.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class CreateCategoryDTO {
    private String categoryName;
    private String categoryType;
    private UUID parentId;
}

package swp391.aistudyhub.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class StartSessionDTO {
    private UUID userId;
    private UUID documentId;
}
